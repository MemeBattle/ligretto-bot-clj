(ns ligretto-bot-clj.core
  (:require [taoensso.timbre :as log]
            [integrant.core :as ig]

            [ligretto-bot-clj.config :as config]

            [ligretto-bot-clj.web.app]
            [ligretto-bot-clj.web.server])
  (:gen-class))

(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(defonce system (atom nil))

(defn stop-app []
  (some->
    (deref system)
    (ig/halt!)))

(defn start-app
  [& {:keys [config] :or {config {:profile :prod}}}]
  (when @system
    (stop-app))
  (->> (config/load-config config)
       (ig/prep)
       (ig/init)
       (reset! system)))

(comment
  (start-app :config {:profile :dev}))

(defn -main
  "I don't do a whole lot ... yet."
  [& _]
  (start-app))
