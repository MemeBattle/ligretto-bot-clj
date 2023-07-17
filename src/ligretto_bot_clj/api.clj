(ns ligretto-bot-clj.api
  (:require [clj-http.client :as http]
            [taoensso.timbre :as log]
            [cheshire.core :as json]
            [clojure.core.async :as async :refer [thread]]))

(def api-url "https://core.ligretto.app/api")

(defn async-request
  [opts]
  (thread (try
            (http/request opts)
            (catch Exception e
              (log/error opts e)
              (throw e)))))

(defn get-me
  ([token]
   (async-request {:url    (str api-url "/auth/me")
                   :method :post
                   :as     :json
                   :body   (json/generate-string {:token token})}))
  ([] (get-me nil)))
