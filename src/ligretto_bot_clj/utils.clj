(ns ligretto-bot-clj.utils)

(defn uuid []
  (str (java.util.UUID/randomUUID)))

(defn find-index
  [pred coll]
  {:pre [fn? pred]}
  (let [idx (first (keep-indexed #(when (pred %2) %1) coll))]
    (if (nil? idx)
      (count coll)
      idx)))

(defn find-first
  [pred coll]
  {:pre [fn? pred]}
  (first (filter pred coll)))
