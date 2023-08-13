(ns ligretto-bot-clj.web.controllers.bot
  (:require
   [hiccup.page :refer [html5]]
   [hiccup.core :refer [html]]

   [ligretto-bot-clj.web.views.home :refer [BotList home-page WithError]]
   [ligretto-bot-clj.web.services.bot :as bot-service]))

(defn home
  [{:keys [ctx]}]
  (html5 (home-page ctx)))

(defn create
  [{:keys [params ctx]}]
  (let [room-url (get params "room-url")
        strategy (get params "strategy")
        turn-timeout (get params "turn-timeout")]
   (try
     (let [game-id (bot-service/extract-game-id room-url)]
      (bot-service/create
       {:game-id game-id
        :strategy strategy
        :turn-timeout turn-timeout}
       ctx))
       (html (BotList ctx))
     (catch Exception e
       (let [ctx (assoc ctx :error (or (ex-message e) "Could not create bot"))]
        (html (WithError ctx (BotList ctx))))))))

(defn all-bots
  [{:keys [ctx]}]
  (html (BotList ctx)))