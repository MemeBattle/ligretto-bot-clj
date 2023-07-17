(ns ligretto-bot-clj.actions
  (:require    [ligretto-bot-clj.socket-io-client :as sic]))

(def action-types
  {:connect-to-room "@@rooms/WEBSOCKET/CONNECT_TO_ROOM"

   :set-player-status "@@game/WEBSOCKET/SET_PLAYER_STATUS"

   :put-card "@@gameplay/WEBSOCKET/PUT_CARD"
   :put-card-from-stack "@@gameplay/WEBSOCKET/PUT_CARD_FROM_STACK_OPEN_DECK"

   :take-card-from-ligretto-deck "@@gameplay/WEBSOCKET/TAKE_FROM_LIGRETTO_DECK"
   :take-card-from-stack "@@gameplay/WEBSOCKET/TAKE_FROM_STACK_DECK"})

(defn ->action
  [type payload]
  {:type (action-types type)
   :payload payload})

(defn emit-action!
  [socket action]
  (sic/emit! socket "message" action))
