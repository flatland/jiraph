(ns jiraph.masai-sorted-test
  (:use clojure.test jiraph.graph
        [useful.utils :only [adjoin]])
  (:require [gloss.core :as gloss]
            [jiraph.masai-sorted-layer :as masai]
            [jiraph.layer :as layer]
            [clojure.walk :as walk]))

(defn =*
  "Compare for equality, but treating nil the same as an empty collection."
  [& args]
  (let [nil? #(or (nil? %) (= % {}))]
    (apply = (for [x args]
               (walk/postwalk (fn [x]
                                (cond (nil? x) nil
                                      (map? x) (into (empty x)
                                                     (for [[k v] x
                                                           :when (not (nil? v))]
                                                       [k v]))
                                      :else x))
                              x)))))

(deftest stuff-works
  ;; these paths are all writing with the default codec:
  ;; cereal's revisioned clojure-reader codec
  (let [= =*] ;; jiraph treats {} and nil equivalently; test must account for this
    (masai/with-temp-layer [layer :formats {:node [[[:edges :*]]
                                                   [[:names]] ;; TODO support indexing non-maps
                                                   [[]]]}]
      (let [id "profile-1"
            init-node {:edges {"profile-10" {:rel :child}}
                       :age 24, :names {:first "Clancy"}}
            node-2 (assoc-in init-node [:edges "profile-21"] {:bond :strong})
            node-3 (update-in node-2 [:names] dissoc :first)

            change-4 {:names {:first "Clancy"}
                      :edges {"profile-10" {:rel :partner}}}
            node-4 (adjoin node-3 change-4)]

        (is (update-in-node! layer [id] adjoin init-node))
        (is (= init-node (get-node layer id)))

        (is (update-in-node! layer [id :edges] assoc "profile-21" {:bond :strong}))
        (is (= node-2 (get-node layer id)))

        (is (update-in-node! layer [id :names] dissoc :first))
        (is (= node-3 (get-node layer id)))

        (is (update-in-node! layer [id] adjoin change-4))
        (is (= node-4 (get-node layer id)))))))
