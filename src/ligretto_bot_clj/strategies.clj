(ns ligretto-bot-clj.strategies
  (:require [ligretto-bot-clj.actions :as actions :refer [->action]]
            [ligretto-bot-clj.utils :as utils :refer [find-index find-first]]
            [clojure.core.async :as async :refer [<! go timeout]]))

(def ^:const default-turn-timeout 1500)

(defn put-card ([ctx from-index to-index]
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
     :playground (->> (get-in game [:playground :decks])
                      (map #(last (:cards %))))}))

(defn can-put-card?
  [card playground]
  (let [{:keys [color value]} card]
    (if (= value 1)
      true
      (some #(and (= (:color %) color)
                  (= (:value %) (dec value)))
            playground))))

(defn find-place-to-put
  "Returns index on playground to put card"
  [card playground]
  (let [{:keys [color value]} card]
    (if (= 1 value)
      (count playground)
      (find-index #(and (= (:color %) color)
                        (= (:value %) (dec value)))
                  playground))))

(defn turn-timeout
  [ctx]
  (timeout (or (:turn-timeout ctx) default-turn-timeout)))

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
      random-sample)))

(defmethod random-action :put-card-from-stack
  [ctx]
  (let [{:keys [stack playground]} (extract-decks ctx)
        open-card (last stack)]
    (when (can-put-card? open-card playground)
      (put-card-from-stack ctx (dec (count playground))))))

(defmethod random-action :take-card-from-stack
  [ctx]
  (take-card-from-stack ctx))

(defmethod random-action :put-card-from-liretto
  [ctx]
  (let [{:keys [ligretto playground]} (extract-decks ctx)
        playable-cards-indexed (keep-indexed
                                (fn [i card]
                                  (when (can-put-card? card playground)
                                    [card i]))
                                ligretto)
        [random-card random-card-index] (rand-nth playable-cards-indexed)]
    (when random-card
      (let [index (find-place-to-put random-card playground)]
        (put-card ctx random-card-index index)))))

(defmethod random-action :take-card-from-ligretto-deck
  [ctx]
  (take-card-from-ligretto-deck ctx))

(defmethod random-action :default
  [ctx]
  ctx)

(defmethod make-turn :random
  [ctx]
  (go
    (<! (turn-timeout ctx))
    (random-action ctx)))

(defn default-turn-action
  [ctx]
  (let [{:keys [stack playground]} (extract-decks ctx)
        open-card (last stack)]
    (if (can-put-card? open-card playground)
      (if (= 1 (:value open-card))
        (put-card-from-stack ctx)
        (put-card-from-stack ctx (find-place-to-put open-card playground)))
      (take-card-from-stack ctx))))

(defmethod make-turn :default
  [ctx]
  (go
    (<! (turn-timeout ctx))
    (default-turn-action ctx)))
