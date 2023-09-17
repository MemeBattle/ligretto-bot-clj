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
  (log/set-min-level! :error)
  (log/with-min-level :info
    (start-app :config {:profile :dev}))

  (require '[portal.api :as p])
  (add-tap #'p/submit)
  (p/start {:port 3000})
  (tap> "test")

  (tap> @system)
  @system)

(defn shutdown-hook []
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. ^Runnable
                     (fn []
                       (println "Shutting Down!")
                       (stop-app)
                       (shutdown-agents)))))

(defn -main
  [& _]
  (shutdown-hook)
  (log/with-min-level :error
    (start-app :config {:profile :prod})))
