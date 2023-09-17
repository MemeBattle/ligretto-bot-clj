(ns ligretto-bot-clj.web.services.bot-test
  (:require [clojure.core.async :refer [chan]]
            [clojure.test :refer :all]
            [ligretto-bot-clj.web.services.bot :as sut]))

(deftest bot-services-test
  (testing "extract-game-id"

    (is (= (sut/extract-game-id "https://ligretto.app/game/f11b95f6-fe13-49e5-b9d3-7c2f42b571b4")
           "f11b95f6-fe13-49e5-b9d3-7c2f42b571b4")))

  (testing "remove-bot-by-id"
    (let [db (atom {:123 {:321 {:stoped? (promise) :events> (chan)}}})]
      (sut/remove-bot "321" {:db db})
      (is (= @db {:123 {}})))))
