(ns ligretto-bot-clj.web.server
  (:require [integrant.core :as ig]
            [ring.adapter.jetty :refer [run-jetty]]))

(defmethod ig/init-key :web/server
  [_ {:keys [app config]}]
  (run-jetty app config))

(defmethod ig/halt-key! :web/server
  [_ server]
  (.stop server))
