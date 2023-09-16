(defproject ligretto-bot-clj "0.1.0-SNAPSHOT"
  :description "Bot server for ligretto.app"
  :url "https://ligretto.app/"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/core.async "1.6.673"]
                 [cheshire "5.11.0"]
                 [clj-http "3.12.3"]
                 [clj-http-fake "1.0.4"]
                 [camel-snake-kebab "0.4.3"]
                 [aero "1.1.6"]
                 [compojure "1.7.0"]
                 [ring "1.10.0"]
                 [ring/ring-json "0.5.1"]
                 [ring/ring-defaults "0.3.4"]
                 [ring-logger "1.1.1"]
                 [hiccup "1.0.5"]
                 [integrant "0.8.1"]
                 [com.taoensso/timbre "6.2.1"]
                 [io.socket/socket.io-client "2.1.0"]]
  :main ^:skip-aot ligretto-bot-clj.core
  :repl-options {:init-ns ligretto-bot-clj.core}

  :uberjar-name "app.jar"
  :resourses-path "resources"

  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
