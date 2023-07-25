(ns ligretto-bot-clj.web.app
  (:require [integrant.core :as ig]
            [ring.logger :refer [wrap-with-logger]]
            [taoensso.timbre :as log]
            [ligretto-bot-clj.web.router :refer [router]]
            [ligretto-bot-clj.web.middleware :refer [wrap-request-ctx wrap-ignore-trailing-slash]]
            [ligretto-bot-clj.bot.bot :refer [stop-bot]]))

;; DB structure
;; { game-id { bot-id { bot } } }
(defmethod ig/init-key :web/db
  [_ {:keys [init-value] :or {init-value {}}}]
  (atom init-value))

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
        (wrap-request-ctx *ctx*))))
