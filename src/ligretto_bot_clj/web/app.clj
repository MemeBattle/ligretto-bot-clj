(ns ligretto-bot-clj.web.app
  (:require [integrant.core :as ig]
            [ring.logger :refer [wrap-with-logger]]
            [ring.middleware.resource :refer [wrap-resource]]
            [taoensso.timbre :as log]
            [clojure.core.async :as async]

            [ligretto-bot-clj.web.router :refer [router]]
            [ligretto-bot-clj.web.middleware :refer [wrap-request-ctx wrap-ignore-trailing-slash]]
            [ligretto-bot-clj.bot.bot :refer [stop-bot]]))

(defn clear-db-worker
  [db]
  (log/info "Clearing DB")
  (doseq [[game-id game] @db]
    (if (empty? game)
      (swap! db dissoc game-id)
      (doseq [bot (vals game)]
        (when (realized? (:stopped? bot))
          (swap! db dissoc (keyword (:bot-id bot))))))))

(defn run-clear-db-worker
  "Runs worker to clear stopped bots from DB
  Every minute"
  [db]
  (async/go-loop []
    (clear-db-worker db)
    (async/<! (async/timeout 10000))
    (recur)))

;; DB structure
;; { game-id { bot-id { bot } } }
(defmethod ig/init-key :web/db
  [_ {:keys [init-value] :or {init-value {}}}]
  (let [db (atom init-value)]
    (run-clear-db-worker db)
    db))

(defmethod ig/halt-key! :web/db
  [_ db]
  (doseq [game (vals @db)]
    (doseq [bot (vals game)]
      (try
        (stop-bot bot)
        (catch Exception e
          (log/error (ex-message e))))))
  (reset! db {}))

(def ^:dynamic *ctx* nil)

(defmethod ig/init-key :web/app
  [_ {:keys [db]}]
  (binding [*ctx* {:db db}]
    (-> router
        (wrap-with-logger {:log-fn (fn [{:keys [level throwable message]}]
                                     (log/log level throwable message))})
        (wrap-ignore-trailing-slash)
        (wrap-resource "public")
        (wrap-request-ctx *ctx*))))
