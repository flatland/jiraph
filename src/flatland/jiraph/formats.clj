(ns flatland.jiraph.formats
  (:use [flatland.useful.experimental :only [lift-meta]]
        [flatland.useful.map :only [update update-each]]
        [flatland.useful.utils :only [copy-meta]])
  (:require [flatland.ego.core :as ego]
            [flatland.schematic.core :as schema]
            [flatland.jiraph.codex :as codex]))

(def reset-key :codec_reset)
(def revision-key :revisions)
(def len-key :proto_length)

(defn tidy-node [node]
  (-> node
      (dissoc reset-key len-key)
      (lift-meta revision-key)))

(defn revisions-codec [codec]
  ;; read revisions just by discarding the rest
  (codex/wrap codec
              nil ;; never write with this codec
              (comp revision-key meta)))

(defn resetting-codec [codec]
  (codex/wrap codec
              (fn [data] (assoc data reset-key true))
              identity))

(defn add-revisioning-modes [format]
  (let [{:keys [codec]} format]
    (-> format
        (assoc :revisions (revisions-codec codec)
               :reset     (resetting-codec codec)))))

;; TODO accept codec+opts as well as codec-fn+reduce-fn
(defn revisioned-format [format-fn]
  ;; TODO take in a map of reduce-fn and (optionally) init-val
  (fn [{:keys [revision] :as opts}]
    (let [{:keys [reduce-fn] :as format} (format-fn opts)]
      (letfn [(reducer [acc x]
                (reduce-fn (if (reset-key x)
                             (select-keys acc [revision-key])
                             acc)
                           (dissoc x reset-key)))
              (combine [items]
                (when (seq items)
                  (tidy-node (or (reduce reducer items) {}))))
              (frame [pre-encode post-decode]
                (-> format
                    (update :codec codex/wrap
                            (comp list pre-encode)
                            (comp combine post-decode))
                    (add-revisioning-modes)))]
        (if revision
          (frame #(assoc % revision-key [revision])
                 (fn [vals]
                   ;; run BEFORE tidy-node, so revision is not in the meta but in the node
                   (take-while #(<= (first (revision-key %)) revision)
                               vals)))
          (frame identity identity))))))

