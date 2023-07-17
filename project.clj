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
                 [com.taoensso/timbre "6.2.1"]
                 [io.socket/socket.io-client "2.1.0"]]
  :main ^:skip-aot ligretto-bot-clj.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
