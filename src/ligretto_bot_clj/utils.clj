(ns ligretto-bot-clj.utils)

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn find-index
  [pred coll]
  {:pre [fn? pred]}
    (first (keep-indexed #(when (pred %2) %1) coll)))

(defn find-first
  [pred coll]
  {:pre [fn? pred]}
  (first (filter pred coll)))

(defn parse-int-safe
  [^String s]
  (try
    (Integer/parseInt s)
    (catch Exception _
      nil)))
