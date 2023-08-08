(ns ligretto-bot-clj.web.controllers.health
  (:require
    [ring.util.response :refer [response]])
  (:import
    [java.util Date]))

(defn healthcheck!
  [_]
  (response
    {:time     (str (Date. (System/currentTimeMillis)))
     :up-since (str (Date. (.getStartTime (java.lang.management.ManagementFactory/getRuntimeMXBean))))
     :app      {:status  "up"
                :message ""}}))
