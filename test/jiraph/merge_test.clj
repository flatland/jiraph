(ns jiraph.merge-test
  (:use clojure.test jiraph.core)
  (:require [jiraph.masai-layer :as masai]))

(defn empty-graph [f]
  (with-graph {:meta   (masai/make-temp)
               :people (masai/make-temp)}
    (f)))

(use-fixtures :each empty-graph)

(deftest merging
  (testing "nothing is merged initially"
    (is (empty? (merged-into "A")))
    (is (empty? (merged-into "B")))
    (is (= ["A"] (merge-ids "A")))
    (is (= ["B"] (merge-ids "B")))
    (is (= nil (merge-head "A")))
    (is (= nil (merge-head "B"))))

  (testing "merge two nodes"
    (at-revision 1 (merge-node! "A" "B"))
    (is (= #{"B"}    (merged-into "A")))
    (is (= ["A" "B"] (merge-ids "A")))
    (is (= ["A" "B"] (merge-ids "B")))
    (is (= "A" (merge-head "A")))
    (is (= "A" (merge-head "B"))))

  (testing "cannot re-merge tail"
    (is (thrown-with-msg? Exception #"already merged"
          (merge-node! "C" "B"))))

  (testing "cannot merge into non-head"
    (is (thrown-with-msg? Exception #"already merged"
          (merge-node! "B" "C"))))

  (testing "merge multiple nodes into a single head"
    (at-revision 2 (merge-node! "A" "C"))
    (at-revision 3 (merge-node! "A" "D"))
    (is (= #{"B" "C" "D"} (merged-into "A"))))

  (testing "merge two chains together"
    (at-revision 4 (merge-node! "E" "F"))
    (at-revision 5 (merge-node! "E" "G"))
    (is (= #{"F" "G"} (merged-into "E")))
    (at-revision 6 (merge-node! "A" "E"))
    (is (= #{"F" "G"} (merged-into "E")))
    (is (= #{"B" "C" "D" "E" "F" "G"} (merged-into "A"))))

  (testing "unmerge latest merge"
    (at-revision 7 (unmerge-node! "A" "E"))
    (is (= nil (merge-head "E")))
    (is (= #{"F" "G"} (merged-into "E")))
    (is (= #{"B" "C" "D"} (merged-into "A")))))

(deftest edge-merging
  (at-revision 1 (assoc-in-node! :people ["A" :edges] {"B" {:foo 1} "C" {:foo 2}}))
  (at-revision 2 (merge-node! "C" "B"))

  (is (= {"C" {:foo 2}} (get-in-node :people ["A" :edges])))
  (is (= #{"A"} (get-incoming :people "C")))
  (is (= #{"A"} (get-incoming :people "B")))

  (at-revision 3 (unmerge-node! "C" "B"))
  (at-revision 4 (merge-node! "B" "C"))

  (is (= {"B" {:foo 1}} (get-in-node :people ["A" :edges])))
  (is (= #{"A"} (get-incoming :people "C")))

  (at-revision 5 (assoc-node! :people "D" {:a 1 :edges {"F" {:foo 1 :bar 2 :baz 3}}}))
  (at-revision 6 (assoc-node! :people "E" {:a 2 :edges {"G" {:foo 3 :baz nil}}}))
  (at-revision 7 (merge-node! "D" "E"))
  (at-revision 8 (merge-node! "G" "F"))

  (is (= {:a 1 :edges {"G" {:foo 3 :bar 2 :baz nil}}} (get-node :people "D")))
  (is (= {:a 1 :edges {"G" {:foo 3 :bar 2 :baz nil}}} (get-node :people "E")))
  (is (= #{"D"} (get-incoming :people "G")))
  (is (= #{"D"} (get-incoming :people "F")))

  (at-revision 9  (unmerge-node! "D" "E"))
  (at-revision 10 (unmerge-node! "G" "F"))

  (is (= {:a 1 :edges {"F" {:foo 1 :bar 2 :baz 3}}} (get-node :people "D")))
  (is (= {:a 2 :edges {"G" {:foo 3 :baz nil}}}      (get-node :people "E")))
  (is (= #{"E"} (get-incoming :people "G")))
  (is (= #{"D"} (get-incoming :people "F"))))

(deftest deleted-edge-merging-opposite-direction
  (at-revision 1 (assoc-node! :people "A" {:edges {"C" {:deleted true}}}))
  (at-revision 2 (assoc-node! :people "B" {:edges {"D" {:deleted false}}}))
  (at-revision 3 (merge-node! "A" "B"))
  (at-revision 4 (merge-node! "D" "C"))

  (is (= {:edges {"D" {:deleted false}}} (get-node :people "A")))
  (is (= {:edges {"D" {:deleted false}}} (get-node :people "B")))
  (is (= {"A" true} (get-incoming-map :people "C")))
  (is (= {"A" true} (get-incoming-map :people "D"))))

(deftest deleted-edge-merging-same-direction
  (at-revision 1 (assoc-node! :people "A" {:edges {"C" {:deleted true}}}))
  (at-revision 2 (assoc-node! :people "B" {:edges {"D" {:deleted false}}}))
  (at-revision 3 (merge-node! "A" "B"))
  (at-revision 4 (merge-node! "C" "D"))

  (is (= {:edges {"C" {:deleted true}}} (get-node :people "A")))
  (is (= {:edges {"C" {:deleted true}}} (get-node :people "B")))
  (is (= {"A" false} (get-incoming-map :people "C")))
  (is (= {"A" false} (get-incoming-map :people "D"))))