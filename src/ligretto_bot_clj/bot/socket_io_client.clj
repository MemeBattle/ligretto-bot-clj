(ns ligretto-bot-clj.bot.socket-io-client
  (:require [clojure.pprint :as pprint]
            [taoensso.timbre :as log]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [clojure.core.async :as async :refer [>! go chan close!]]
            [clojure.core.memoize :as m]
            [clojure.walk :as walk])
  (:import [org.json JSONObject]
           [io.socket.client IO Socket]
           [io.socket.emitter Emitter$Listener]))

(deftype Listener [callback]
  Emitter$Listener
  (call [& args]
    (apply callback args)))

(def memoized->camelCaseString
  (m/fifo csk/->camelCaseString {} :fifo/threshold 512))

(defn- make-args
  [message hash]
  (cond (or (list? message) (vector? message) (seq? message)) message
        (map? message) (let [json (JSONObject.)
                             message* (cske/transform-keys memoized->camelCaseString message)]
                         (when hash (.put json "hash" hash))
                         (doseq [[k v] message*]
                           (.put json (name k) v))
                         (log/debug (format "json msg: %s" json))
                         [json])
        :else [message]))

(defn emit!
  ([^Socket socket ^String event msg hash]
   (let [args (make-args msg hash)]
     (.emit socket event (into-array Object args))
     hash))
  ([socket event msg]
   (emit! socket event msg nil)))

(defn connect!
  [^Socket socket]
  (.connect socket))

(defn disconnect!
  [^Socket socket]
  (.disconnect socket))

(def options->fn
  {:path (fn [io-options val]
           (.setPath io-options val))
   :reconnection (fn [io-options val]
                   (.setReconnection io-options val))
   :auth (fn [io-options val]
           (.setAuth io-options (walk/stringify-keys val)))
   :transports (fn [io-options val]
                 (.setTransports io-options (into-array String val)))})

(defn make-io-options [options]
  (let [io-options (io.socket.client.IO$Options/builder)]
    (doseq [[opt-key opt-val] options]
      (when-let [opt-fn (options->fn opt-key)]
        (opt-fn io-options opt-val)))
    (.build io-options)))

(defn make-socket
  ([url event-map options]
   {:pre [(string? url)]}
   (let [socket> (chan)
         event-map (walk/stringify-keys event-map)
         io-options  (make-io-options options)
         socket      (IO/socket url io-options)
         event-map*  (merge event-map {Socket/EVENT_CONNECT (fn [& args]
                                                              (go
                                                                (log/debug (format "connected args %s" (apply str args)))
                                                                (log/info (format "connected to %s" url))
                                                                (>! socket> socket)))
                                       Socket/EVENT_CONNECT_ERROR (fn [error]
                                                                    (pprint/pprint error)
                                                                    (go
                                                                      (log/error (format "failed to connect to %s" url)
                                                                                 (log/debug (format "%s" (.getMessage error)) options)
                                                                                 (close! socket>))))})]
     (.open socket)

     (doseq [[event handler] event-map*]
       (.. socket
           (on event (->Listener handler))))

     (connect! socket)

     socket>))

  ([url] (make-socket url {} {}))
  ([url event-map] (make-socket url event-map {})))
