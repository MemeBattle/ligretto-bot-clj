(ns ligretto-bot-clj.utils-test
  (:require [clojure.test :refer :all]
            [ligretto-bot-clj.utils :as sut]))

(deftest utils-test
  (testing "find-index"

    (is (= (sut/find-index #(= % 1) [1 2 3])
           0)))

  (testing "find-first"

    (is (= (sut/find-first #(= % 3) [1 2 3])
           3))))
