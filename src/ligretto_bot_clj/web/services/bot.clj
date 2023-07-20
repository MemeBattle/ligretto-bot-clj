(ns ligretto-bot-clj.web.services.bot
  (:require
            [clojure.core.async :as async]

            [ligretto-bot-clj.bot.bot :refer [create-bot]]))


;;

(defn ->bot
  [bot]
  (let [{:keys [id strategy turn-timeout user]} bot]
    {:id id
     :strategy strategy
     :turn-timeout turn-timeout
     :user user}))

(defn ->games
  [db]
  (let [entries (seq db)]
    (map (fn [[game-id bots]]
           (let [[_ bot] (first bots)
                 game-state @(:game-state bot)]
             {:game-id game-id :game-state game-state})) entries)))

(defn ->bots
  [db]
  (into {} (map (fn [[game-id bots]]
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
  (let [game (get @db game-id)]
    (when (nil? game)
      (swap! db assoc (keyword game-id) {}))
    (let [bot
          (async/<!! (create-bot game-id {:strategy strategy :turn-timeout turn-timeout}))]
      (swap! db assoc-in [game-id (:bot-id bot)] bot)
      (->bot bot))))

(defn get-by-game-id
  [game-id {:keys [db]}]
  (let [bots (get @db game-id)]
    (map ->bot bots)))
