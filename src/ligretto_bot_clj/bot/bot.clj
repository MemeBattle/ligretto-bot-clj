(ns ligretto-bot-clj.bot.bot
  (:require
   [cheshire.core :as json]
   [taoensso.timbre :as log]
   [camel-snake-kebab.core :as csk]
   [clojure.core.async :as async :refer [<! >! go go-loop chan close! alts! timeout]]
   [ligretto-bot-clj.constants :refer [socket-url]]
   [ligretto-bot-clj.utils :as utils]
   [ligretto-bot-clj.bot.socket-io-client :as sic]
   [ligretto-bot-clj.bot.api :as api]
   [ligretto-bot-clj.bot.strategies :as strat]
   [ligretto-bot-clj.bot.actions :as actions :refer [emit-action! ->action]]))

(def ^:const start-game-timeout 120000) ;; 2 minutes
(def ^:const update-game-timeout 180000) ;; 3 minutes
(def ^:const connect-timeout 15000) ;; 15 seconds

(def event-types
  {:connect-to-room-success "@@rooms/SERVER/CONNECT_TO_ROOM_SUCCESS"
   :connect-to-room-failure "@@rooms/SERVER/CONNECT_TO_ROOM_ERROR"

   :update-game "@@game/SERVER/UPDATE_GAME"

   :end-round "@@gameplay/SERVER/END_ROUND"
   :user-join-to-room "@@game/SERVER/USER_JOIN_TO_ROOM"})

(defn handle-event
  [port]
  (fn [event]
    (let [event* (-> event
                     (.toString)
                     (json/parse-string csk/->kebab-case-keyword))]
      (go
        (log/debugf "event: %s" event*)
        (>! port event*)))))

(defn extract-game
  [message]
  (-> message
      :payload
      :game))

(def game-statuses
  {:new "New"
   :starting "Starting"
   :in-game "InGame"
   :pause "Pause"
   :round-finished "RoundFinished"})

(defn game-status=
  [status ctx]
  (= (game-statuses status) (-> ctx :game-state deref :status)))

(defn wait-for-event
  [event-type events>]
  (go-loop []
    (let [event (<! events>)]
      (if (= (:type event) (event-types event-type))
        event
        (recur)))))

(defn wait-for-start-game
  [events>]
  (go-loop []
    (let [update-event (<! (wait-for-event :update-game events>))
          game-status (-> update-event
                          :payload
                          :status)]
      (if (= (game-statuses :in-game) game-status)
        update-event
        (recur)))))

(defn stop-bot
  [ctx]
  (let [{:keys [socket status events>]} ctx]
    (when (not (nil? socket))
     (sic/disconnect! socket))
    (reset! status :shutdown)
    (close! events>)
    (log/infof "[%s] Bot stopped" (:bot-id ctx))))

(defn handle-updates
  [ctx]
  (go-loop []
    (let [update-timeout> (timeout update-game-timeout)
          [event ch] (alts! [(:events> ctx)
                             update-timeout>])]
      (if (= ch update-timeout>)
        (do
          (log/errorf "[%s] Update game timeout" (:bot-id ctx))
          (stop-bot ctx))
        (condp = (:type event)
          (event-types :update-game)
          (do
            (reset! (:game-state ctx) (:payload event))
            (log/debugf "[%s] Game updated" (:bot-id ctx))
            (recur))

          (event-types :end-round)
          (do
            (log/debugf "[%s] Round finished" (:bot-id ctx))
            (recur))

          (log/errorf "[%s] Unknown event: %s" (:bot-id ctx) event))))))

(defn turn-timeout
  [ctx]
  (timeout (:turn-timeout ctx)))

(defn game-loop
  [ctx]
  (let [{:keys [bot-id]} ctx]
    (go-loop []
      (<! (turn-timeout ctx))
      (let [turn (strat/make-turn ctx)]
        (when turn
          (emit-action! (:socket ctx) turn))
        (log/debug (format "[%s] Turn: %s" bot-id turn)))
      (when (not (= @(:status ctx) :shutdown))
        (recur)))))

(defn process-game
  [ctx]
  (let [{:keys [bot-id room-id game-state events>]} ctx]
    ;; TODO: support wait for game end as a spectrator, pasued game
    (when (not (game-status= :new ctx))
      (log/errorf "[%s] Game already started" bot-id)
      (stop-bot ctx)
      (throw (ex-info "Game already started" {:room-id room-id :bot-id bot-id})))

    (go
      (let [wait-start-timeout>    (timeout start-game-timeout)
            [stated-game-event ch] (alts! [(wait-for-start-game events>)
                                           wait-start-timeout>])]
        (when (= ch wait-start-timeout>)
          (log/errorf "[%s] Game start timeout" bot-id)
          (stop-bot ctx)
          (throw (ex-info "Game start timeout" {:room-id room-id :bot-id bot-id})))

        (log/infof "[%s] Game started: %s" bot-id room-id)
        (reset! game-state (:payload stated-game-event))
        (reset! (:status ctx) :in-game)

        (handle-updates ctx)
        (game-loop ctx)))))

(defn create-bot
  ([room-id options]
   (go
     (let [me-response (:body (<! (api/get-me)))
           token       (:token me-response)
           user        (:user me-response)
           events>     (chan)
           socket      (<! (sic/make-socket socket-url
                                            {:event (handle-event events>)}
                                            {:auth {:token token}}))
           bot-id    (utils/uuid)
           ctx     {:bot-id       bot-id
                    :game-state   (atom {})
                    :user         user
                    :turn-timeout (or (:turn-timeout options) 1000)
                    :strategy     (or (:strategy options) :easy)
                    :room-id      room-id
                    :socket       socket
                    :status       (atom :new)
                    :events>      events>}]

       (let [timeout> (timeout connect-timeout)
             sucess>  (wait-for-event :connect-to-room-success events>)]

         (emit-action! socket (->action :connect-to-room {:room-uuid room-id}))

         (let [[connected-data ch] (alts! [sucess> timeout>])]
           (condp = ch
             sucess>  (do
                        (log/infof "[%s] Connected to room %s" bot-id room-id)
                        (reset! (:status ctx) :connected)
                        (reset! (:game-state ctx) (extract-game connected-data))
                        (process-game ctx))
             timeout> (do
                        (log/errorf "[%s] Failed to connect to room %s" bot-id room-id)
                        (stop-bot ctx)))))
       ctx)))
  ([room-id]
   (create-bot room-id {})))
