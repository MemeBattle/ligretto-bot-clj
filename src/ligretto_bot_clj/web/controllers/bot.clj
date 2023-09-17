(ns ligretto-bot-clj.web.controllers.bot
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]
            [ligretto-bot-clj.utils :refer [parse-int-safe]]
            [ligretto-bot-clj.web.services.bot :as bot-service]
            [ligretto-bot-clj.web.views.home :refer [BotList home-page
                                                     WithError]]))

(defn home
  [{:keys [ctx]}]
  (html5 (home-page ctx)))

(defn create
  [{:keys [params ctx]}]
  (let [room-url (get params "room-url")
        strategy (keyword (get params "strategy"))
        turn-timeout (parse-int-safe (get params "turn-timeout"))]
   (try
     (let [game-id (bot-service/extract-game-id room-url)
           options {:game-id game-id
                    :strategy strategy
                    :turn-timeout turn-timeout}]
       (bot-service/create options ctx)
       (html (BotList ctx)))
     (catch Exception e
       (let [ctx (assoc ctx :error (or (ex-message e) "Could not create bot"))]
        (html (WithError ctx (BotList ctx))))))))

(defn delete
  [{:keys [params ctx]}]
  (tap> params)
  (let [game-id (:game-id params)
        bot-id (:bot-id params)]
   (try
     (bot-service/remove-bot game-id bot-id ctx)
     (html (BotList ctx))
     (catch Exception e
       (let [ctx (assoc ctx :error (or (ex-message e) "Could not delete bot"))]
        (html (WithError ctx (BotList ctx))))))))

(defn all-bots
  [{:keys [ctx]}]
  (html (BotList ctx)))
