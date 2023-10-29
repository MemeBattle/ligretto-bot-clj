(ns ligretto-bot-clj.bot.strategies
  (:require [ligretto-bot-clj.bot.actions :as actions :refer [->action]]
            [ligretto-bot-clj.utils :as utils :refer [find-index find-first]]))

(defn put-card
  ([ctx from-index to-index]
   (let [game-id (:room-id ctx)]
     (->action :put-card {:game-id               game-id
                          :playground-deck-index to-index
                          :card-index            from-index})))
  ([ctx from-index]
   (let [game-id (:room-id ctx)]
     (->action :put-card {:game-id               game-id
                          :card-index            from-index}))))

(defn put-card-from-stack
  ([ctx to-index]
   (let [game-id (:room-id ctx)]
     (->action :put-card-from-stack {:game-id               game-id
                                     :playground-deck-index to-index})))
  ([ctx]
   (let [game-id (:room-id ctx)]
     (->action :put-card-from-stack {:game-id               game-id}))))

(defn take-card-from-ligretto-deck [ctx]
  (let [game-id (:room-id ctx)]
    (->action :take-card-from-ligretto-deck {:game-id game-id})))

(defn take-card-from-stack [ctx]
  (let [game-id (:room-id ctx)]
    (->action :take-card-from-stack {:game-id game-id})))

(defn extract-decks [ctx]
  (let [user-id (-> ctx :user :casId)
        game @(:game-state ctx)
        players (:players game)
        players-kw (into [] players)
        [_ player] (find-first (fn [[_ player]] (= user-id (:id player))) players-kw)]

    {:ligretto (:cards player)
     :stack (get-in player [:stack-open-deck :cards])
     :playground (some->> (get-in game [:playground :decks])
                      (mapv #(some-> % :cards last)))}))

(defn can-put-card?
  [card playground]
  (let [{:keys [color value]} card]
    (if
      (= value 1) true
      (some #(and
              (not (nil? (:value %)))
              (= (:color %) color)
              (= (:value %) (dec value)))
            playground))))

(defn find-place-to-put
  "Returns index on playground to put card"
  [card playground]
  (let [{:keys [color value]} card]
    (if (= value 1)
      (find-index #(nil? %) playground)
      (find-index #(and (= (:color %) color)
                        (= (:value %) (dec value)))
                  playground))))

(defmulti make-turn
  :strategy)

(def random-actions
  [:put-card-from-stack
   :put-card-from-liretto
   :take-card-from-ligretto-deck
   :take-card-from-stack])

(defmulti random-action
  (fn [_]
    (let [random-sample (rand-nth random-actions)]
      random-sample))
  :default identity)

(defmethod random-action :put-card-from-stack
  [ctx]
  (let [{:keys [stack playground]} (extract-decks ctx)
        open-card (last stack)]
    (when (can-put-card? open-card playground)
      (put-card-from-stack ctx (dec (count playground))))))

(defmethod random-action :take-card-from-stack
  [ctx]
  (take-card-from-stack ctx))

(defmethod random-action :take-card-from-ligretto-deck
  [ctx]
  (take-card-from-ligretto-deck ctx))

(defmethod make-turn :random
  [ctx]
  (let [{:keys [ligretto playground]} (extract-decks ctx)
        playable-cards-indexed
        (keep-indexed
         (fn [i card]
           (when (can-put-card? card playground)
             [card i]))
         ligretto)
        [random-card random-card-index] (rand-nth playable-cards-indexed)]
    (when random-card
      (let [index (find-place-to-put random-card playground)]
        (put-card ctx random-card-index index)))))

(defmethod make-turn :default
  [ctx]
  (let [{:keys [stack playground]} (extract-decks ctx)
        open-card (last stack)]
    (if (can-put-card? open-card playground)
      (if (= (:value open-card) 1)
        (put-card-from-stack ctx)
        (put-card-from-stack ctx (find-place-to-put open-card playground)))
      (take-card-from-stack ctx))))

(defn need-take-from-ligretto-deck?
  "We need to take cards from ligretto deck if we have not enought cards in the ligretto row.
  It sends null for empty card"
  [ligretto]
  (some nil? ligretto))

(defn can-put-card-from-ligretto?
  [ligretto playground]
  (some #(can-put-card? % playground) ligretto))

(defn can-put-card-from-stack?
  [stack playground]
  (let [open-card (last stack)]
    (and (not (nil? open-card)) (can-put-card? open-card playground))))

(defn put-card-from-liretto
  [ctx]
  (let [{:keys [ligretto playground]} (extract-decks ctx)
        playable-cards-indexed
        (keep-indexed
         (fn [i card]
           (when (can-put-card? card playground)
             [card i]))
         ligretto)
        ;; we want to put card with the highest value
        [card card-index] (last (sort-by :value playable-cards-indexed))]
    (when card
      (let [index (find-place-to-put card playground)]
        (put-card ctx card-index index)))))

(defmethod make-turn :easy
  [ctx]

  (let [{:keys [stack playground ligretto]} (extract-decks ctx)]
     (cond
       (need-take-from-ligretto-deck? ligretto) (take-card-from-ligretto-deck ctx)
       (can-put-card-from-ligretto? ligretto playground) (put-card-from-liretto ctx)
       (can-put-card-from-stack? stack playground) (put-card-from-stack ctx)
       :else (take-card-from-stack ctx))))
