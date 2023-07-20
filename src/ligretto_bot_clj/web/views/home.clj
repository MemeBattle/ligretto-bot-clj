(ns ligretto-bot-clj.web.views.home
  (:require [ligretto-bot-clj.web.views.layout :refer [Layout]]))

(defn Bot
  []
  [:div [:p "Bot"]])

(defn home-page
  [ctx]
  (Layout "Ligretto Bot" [:div [:h1 "Ligretto Bot"]]))
