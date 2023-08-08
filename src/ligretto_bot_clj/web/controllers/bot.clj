(ns ligretto-bot-clj.web.controllers.bot
  (:require
   [ring.util.response :refer [redirect]]
   [ligretto-bot-clj.web.services.bot :as bot-service]))

(defn create
  [{:keys [params ctx]}]
  (tap> params)
  (let [room-url (get params "room-url")
        strategy (get params "strategy")
        turn-timeout (get params "turn-timeout")]
   (when-let [game-id (bot-service/extract-game-id room-url)]
   (tap> game-id)
     (bot-service/create
      {:game-id game-id
       :strategy strategy
       :turn-timeout turn-timeout}
      ctx)))
  (redirect "/"))
