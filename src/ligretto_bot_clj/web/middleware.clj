(ns ligretto-bot-clj.web.middleware
  (:require [taoensso.timbre :as log]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.defaults :refer [site-defaults api-defaults wrap-defaults]]
            [camel-snake-kebab.core :as csk]))

(defn assoc-to-request
  [handler key value]
  (fn [request]
    (handler (assoc request key value))))

(defn wrap-request-ctx
  [handler ctx]
  (assoc-to-request handler :ctx ctx))

(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error (ex-message e) {:data (ex-data e) :request request})
        {:status 500
         :body {:message (ex-message e)
                :data (ex-data e)}}))))

(defn wrap-api
  [handler]
  (-> handler
      (wrap-defaults api-defaults)
      (wrap-json-body {:key-fn csk/->kebab-case-keyword})

      (wrap-fallback-exception)

      (wrap-json-response {:key-fn csk/->camelCaseString})))

(defn wrap-ignore-trailing-slash
  "Modifies the request uri before calling the handler.
  Removes a single trailing slash from the end of the uri if present.

  Useful for handling optional trailing slashes until Compojure's route matching syntax supports regex.
  Adapted from http://stackoverflow.com/questions/8380468/compojure-regex-for-matching-a-trailing-slash"
  [handler]
  (fn [request]
    (let [uri (:uri request)]
      (handler (assoc request :uri (if (and (not (= "/" uri))
                                            (.endsWith uri "/"))
                                     (subs uri 0 (dec (count uri)))
                                     uri))))))

(defn wrap-pages
  [handler]
  (-> handler
      (wrap-defaults site-defaults)))
