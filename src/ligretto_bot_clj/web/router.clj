(ns ligretto-bot-clj.web.router
  (:require [compojure.core :refer [defroutes context GET POST]]
            [compojure.route :as route]
            [ring.util.response :refer [response]]

            [ligretto-bot-clj.web.services.bot :as bot-service]
            [ligretto-bot-clj.web.middleware :refer [wrap-api]]))

(defroutes router
  (GET "/" [] "Hello, World!")

  (wrap-api
   (context "/api" {:keys [ctx]}
     (GET "/bots" [] (response (bot-service/get-all ctx)))
     (POST "/bots" {:keys [body]} (response (bot-service/create body ctx)))
     (GET "/bots/game/:game-id" [game-id] (response (bot-service/get-by-game-id game-id ctx)))))

  (route/not-found "Not Found"))
