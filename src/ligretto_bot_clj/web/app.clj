(ns ligretto-bot-clj.web.app
  (:require [integrant.core :as ig]
            [taoensso.timbre :as log]
            [clojure.core.async :as async]

            [ligretto-bot-clj.web.router :refer [router]]
            [ligretto-bot-clj.web.middleware :refer [wrap-base]]
            [ligretto-bot-clj.bot.bot :refer [stop-bot]]))

(defn clear-db-worker
  [db]
  (log/info "Clearing DB")
  (doseq [[game-id game] @db]
    (doseq [bot (vals game)]
      (when (= @(:status bot) :shutdown)
        (log/info "Removing bot" (:bot-id bot) "from game" game-id)
        (swap! db dissoc game-id (keyword (:bot-id bot)))))
    (when (empty? game)
      (swap! db dissoc game-id))))

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
  ;; todo: make protocol for dao and use it here
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

(defmethod ig/init-key :web/app
  [_ {:keys [db]}]
  (wrap-base router {:db db}))
