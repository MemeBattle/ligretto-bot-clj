(ns ligretto-bot-clj.web.services.bot
  (:require [clojure.core.async :as async]
            [clojure.string :as str]
            [ligretto-bot-clj.bot.bot :refer [create-bot stop-bot]]
            [ligretto-bot-clj.constants :refer [game-url]]))

(defn extract-game-id
  [url]
  (when (not (str/starts-with? url game-url))
    (throw (ex-info "Invalid room url" {:url url})))
  (let [room-id (str/replace url (str game-url "/") "")]
    (when (str/blank? room-id)
      (throw (ex-info "Invalid room url" {:url url})))
    room-id))

(defn ->bot
  [bot]
  (let [{:keys [bot-id strategy turn-timeout user status]} bot]
    {:id bot-id
     :strategy strategy
     :turn-timeout turn-timeout
     :user user
     :status @status}))

(defn ->games
  [db]
  (let [entries (seq db)]
    (map
     (fn [[game-id bots]]
       (let [[_ bot] (first bots)]
         (when-let [game-state (:game-state bot)]
           {:game-id game-id :game-state @game-state})))
     entries)))

(defn ->bots
  [db]
  (into {}
        (map
         (fn [[game-id bots]]
           [game-id (map ->bot (vals bots))])
         db)))

(defn get-all
  "Response structure:
  { :games [{ :game-id :game-state }]
   :bots { game-id [ bots ] }}"
  [{:keys [db]}]
  (let [db @db
        bots (->bots db)
        games (->games db)]
    {:bots bots
     :games games}))

(defn create
  [{:keys [game-id strategy turn-timeout]} {:keys [db]}]
  (when (nil? game-id)
    (throw (ex-info "Game id is required" {:game-id game-id})))
  (when (or (nil? strategy) (not (keyword? strategy)))
    (throw (ex-info "Strategy should be easy or random or default" {:strategy strategy})))
  (when (or (nil? turn-timeout) (not (integer? turn-timeout)))
    (throw (ex-info "Turn timeout should be number" {:turn-timeout turn-timeout})))

  (let [game-id* (keyword game-id)
        game (get @db game-id*)
        turn-timeout (or turn-timeout 1000)
        strategy (or strategy :easy)]
    (when (nil? game)
      (swap! db assoc game-id* {}))
    (let [bot (async/<!! (create-bot game-id {:strategy (keyword strategy) :turn-timeout turn-timeout}))]
      (when (nil? bot)
        (throw (ex-info "Failed to create bot" {:game-id game-id :strategy strategy :turn-timeout turn-timeout})))
      (swap! db assoc-in [game-id* (keyword (:bot-id bot))] bot)
      (->bot bot))))

(defn remove-bot
  ([bot-id ctx]
   (let  [{:keys [db]} ctx
          bot-id* (keyword bot-id)
          game-id*
          (->> @db
               (filter (fn [[_ bots]]
                         (some? (get bots bot-id*))))
               (keys)
               (first))]
     (remove-bot game-id* bot-id ctx)))

  ([game-id bot-id {:keys [db]}]
   (let [game-id* (keyword game-id)
         bot-id* (keyword bot-id)
         bot (get-in @db [game-id* bot-id*])]
     (when (not (nil? bot))
       (stop-bot bot)
       (swap! db update-in [game-id*] dissoc bot-id*)
       (->bot bot)))))

(defn remove-game
  [game-id {:keys [db]}]
  (let [game-id* (keyword game-id)
        bots (vals (get @db game-id*))]
    (doseq [bot bots]
      (stop-bot bot))
    (swap! db dissoc game-id*)
    (map ->bot bots)))

(defn get-by-game-id
  [game-id {:keys [db]}]
  (let [game-id* (keyword game-id)
        bots (vals (get @db game-id*))]
    (map ->bot bots)))
