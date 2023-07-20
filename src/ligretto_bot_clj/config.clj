(ns ligretto-bot-clj.config
  (:require [integrant.core :as ig]
            [aero.core :as aero]))

(def ^:const system-filename "config.edn")

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defn load-config
  [options]
  (aero/read-config system-filename options))
