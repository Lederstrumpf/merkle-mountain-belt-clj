(ns linked-peaks
  (:require [core]))

(defn internal-node [left height hash parent]
  {:left left
   :height height
   :hash hash
   :parent parent
   :type :peak})

(defn peak-node [left height hash]
  {:left left
   :height height
   :hash hash
   :parent nil
   :type :peak})

(defn range-node [left right hash parent]
  {:left left
   :right right
   :hash hash
   :parent parent
   :type :range})

(defn belt-node [left right hash parent]
  {:left left
   :right right
   :hash hash
   :parent parent
   :type :belt})

(def lastP (atom nil))

;; (def R-count (atom 0))

(def mergeable-stack (atom []))

(def leaf-count (atom 0))

(def node-map (atom {}))
(def node-array (atom []))

(defn pop-mergeable-stack [& [cached]]
  (let [
        mergeable-stack (if cached (:mergeable-stack cached) mergeable-stack)
        pop-item (last @mergeable-stack)]
    (swap! mergeable-stack (comp #(into [] %) drop-last))
    pop-item))

(defn add-mergeable-stack [item & [cached]]
  (let [mergeable-stack (if cached (:mergeable-stack cached) mergeable-stack)]
    (swap! mergeable-stack #(assoc % (count %) item))))

(defn add-internal [item index & [cached]]
  (let [array-len (count @node-array)
        ;; incidentally correct since index is calculated starting at 1 in lieu of 0
        zero-leaves (- index array-len)
        node-array (if cached (:node-array cached) node-array)
        ]
    (swap! node-array concat (repeat zero-leaves 0) (list item))))

(defn reset-all []
 (do
   (reset! node-map {})
   (reset! node-array [])
   (reset! mergeable-stack [])
   (reset! leaf-count 0)
   (reset! lastP nil)))

(:height (get @node-map @lastP))
(identity @lastP)

(defn hop-left [node & target-map]
  (:left (get (or (first target-map) @node-map) node)))

(defn hop-parent [node & target-map]
  (:parent (get (or (first target-map) @node-map) node)))



(comment
  (algo true))

(def algo-1223-cached
  (algo true {
              :node-map (atom @node-map)
              :node-array (atom @node-array)
              :mergeable-stack (atom @mergeable-stack)
              :leaf-count (atom @leaf-count)
              :lastP (atom @lastP)
              }))

(def algo-1223-cached-2
  (algo true {
              :node-map (atom @node-map)
              :node-array (atom @node-array)
              :mergeable-stack (atom @mergeable-stack)
              :leaf-count (atom @leaf-count)
              :lastP (atom @lastP)
              }))

(count @(:node-array algo-1223-cached))
(count @(:node-array algo-1223-cached-2))

(let [
      algo-1223-cached-3 (algo true {
                                     :node-map (atom @node-map)
                                     :node-array (atom @node-array)
                                     :mergeable-stack (atom @mergeable-stack)
                                     :leaf-count (atom @leaf-count)
                                     :lastP (atom @lastP)
                                     })
      ]
  (map (fn [k]
         [k
          (= @(k algo-1223-cached)
             @(k algo-1223-cached-3))])
       (keys algo-1223-cached)
       ))

(count @(:node-array algo-1223-cached))
(count @(:node-array algo-1223-cached-2))

(def algo-1222-nested (merge
                       (play-algo-with-oneshot-nesting 1222 true)
                       {
                        :mergeable-stack (atom @mergeable-stack)
                        :leaf-count (atom @leaf-count)
                        :lastP (atom @lastP)
                        }))

(def algo-1223-nested (merge
                       (play-algo-with-oneshot-nesting 1223 true)
                       {
                        :mergeable-stack (atom @mergeable-stack)
                        :leaf-count (atom @leaf-count)
                        :lastP (atom @lastP)
                        }))

(defn algo [upgrade? & [cached]]
  (let [
        node-map (if cached (:node-map cached) node-map)
        node-array (if cached (:node-array cached) node-array)
        mergeable-stack (if cached (:mergeable-stack cached) mergeable-stack)
        leaf-count (if cached (:leaf-count cached) leaf-count)
        lastP (if cached (:lastP cached) lastP)

        ;; impermanent state
        ;; range-nodes (if cached (:range-nodes cached) (throw (new Exception "not provided")))
        ;; belts (if cached (:belts cached) (throw (new Exception "not provided")))
        ;; belt-children (if cached (:belt-children cached) (throw (new Exception "not provided")))
        range-nodes (if cached (:range-nodes cached))
        belts (if cached (:belts cached))
        belt-children (if cached (:belt-children cached))

        ;; let h be hash of new leaf
        ;; h (str @leaf-count "-hash")
        h #{@leaf-count}
        ;; create object P, set P.hash<-h, set P.height<-0, set P.left<-lastP
        P (peak-node (:hash (get @node-map @lastP)) 0 h)
        ]
    (do
      ;; 1. Add step
      ;; store object P in peak map
      (swap! node-map #(assoc % h P))
      ;; A[R*n+1]<-h
      (add-internal h (* 2 @leaf-count) cached)

      ;; 2. Check mergeable
      ;; if lastP.height==0 then M.add(P)
      (if (= (:height (get @node-map @lastP)) 0)
        (add-mergeable-stack (get @node-map h) cached))

      ;; 3. reset lastP
      (reset! lastP h)

      ;; 4. merge if mergeable
      (if (not (zero? (count @mergeable-stack)))
        (do
          (let [Q (pop-mergeable-stack cached)
                Q (update Q :height inc)
                L (get @node-map (:left Q))
                Q-old-hash (:hash Q)
                Q (assoc Q :hash (apply sorted-set (concat (:hash L) (:hash Q))))
                Q (assoc Q :left (:left L))]
            ;; add new leaf to node-map
            (swap! node-map #(assoc % (:hash Q) Q))
            ;; update new parent-values
            (swap! node-map #(assoc-in % [(:hash L) :parent] (:hash Q)))
            (swap! node-map #(assoc-in % [Q-old-hash :parent] (:hash Q)))

            ;; change type of children to internal
            (swap! node-map #(assoc-in % [(:hash L) :type] :internal))
            (swap! node-map #(assoc-in % [Q-old-hash :type] :internal))

            (add-internal (:hash Q) (inc (* 2 @leaf-count)) cached)
            ;; issue is that :left of Q can be outdated since may have had subsequent merge
            (if (and (= (:height Q) (:height (get @node-map (:left Q))))
                     ;; TODO: this is dumb and will break for instance if (:left Q) has a left partner they'll still merge with - just temporary fix to see how far we'll get
                     (nil? (:parent (get @node-map (:left Q)))))
              (add-mergeable-stack Q cached))
            ;; if we've replaced the old lastP, should reset lastP to point to the new entry
            (if (= Q-old-hash @lastP)
              (reset! lastP (:hash Q)))
            ;; TODO: the following has a smarter integration
            (if (not upgrade?)
              (do (if (= Q-old-hash (hop-left @lastP node-map))
                    (swap! node-map #(assoc-in % [@lastP :left] (:hash Q))))
                  (if (= Q-old-hash (:left (get @node-map (:left (get @node-map @lastP)))))
                    (swap! node-map #(assoc-in % [(:left (get @node-map @lastP)) :left] (:hash Q)))))
              (let [
                    left-most-sibling-peak (last (take-while #(and (some? %) (nil? (hop-parent % node-map)))
                                                             (iterate (fn [node] (hop-left node node-map)) @lastP)))
                    correct-sibling-of-left-most (take-while some? (iterate (fn [node] (hop-parent node node-map)) (hop-left left-most-sibling-peak node-map)))
                    ]
                (if (and (some? left-most-sibling-peak) (< 1 (count correct-sibling-of-left-most)))
                  (swap! node-map #(assoc-in % [left-most-sibling-peak :left] (last correct-sibling-of-left-most))))))
            )
          )
        )
      (swap! leaf-count inc)

      ;; 5. TODO update range nodes

      ;; check (difference (S-n n) (S-n (dec n)))
      ;; recalculate only those members of S-n that are in the difference set from above

      ;; show results
      ;; (clojure.pprint/pprint [@node-map @node-array @mergeable-stack @lastP])
      ;; (clojure.pprint/pprint @node-map)
      ;; (clojure.pprint/pprint @node-map)
      {
       :node-map node-map
       :node-array node-array
       :mergeable-stack mergeable-stack
       :leaf-count leaf-count
       :lastP lastP
       :belt-children belt-children
       :range-nodes range-nodes
       :belts belts
       }
      ))
  )

(def algo-1222 (play-algo 1222 true))
(def algo-1277 (play-algo 1277 true))
(def algo-1278 (play-algo 1278 true))
(def algo-1279 (play-algo 1279 true))

(filter #(= :peak (:type (get (:node-map algo-1222) (nth (:node-array algo-1222) %))))
        (range (count (:node-array algo-1222))))
(comment (list 1533 1789 2301 2397 2413 2421 2429 2437 2441 2443))

;; NOTE: shifting storage left by 3 since skipping the constant offset from the beginning (always empty)
(map #(- % 3) (sort (storage/parent-less-nodes 1222)))

(defn play-algo-with-oneshot-nesting [n upgrade?]
  (let [
        {:keys [node-map node-array]} (select-keys (play-algo n upgrade?) [:node-map :node-array])
        node-map (atom node-map)
        node-array (atom node-array)
        range-nodes (atom {})
        belt-nodes (atom {})
        sorted-peaks (atom (map #(get @node-map (nth @node-array (- (first %) 3))) (storage/parent-less-nodes-sorted-height (storage/parent-less-nodes n))))
        storage-maps {:peak node-map
                      :range range-nodes
                      :belt belt-nodes}]

    (letfn [(update-parent [parent child]
              (swap! (get storage-maps (:type child)) (fn [storage-map] (assoc-in storage-map [(:hash child) :parent] (:hash parent)))))]
      (let [
            belt-children (doall (map (fn [belt-range-count]
                                        (reduce (fn [left-child right-child]
                                                  (let [rn (range-node (:hash left-child) (:hash right-child)
                                                                       (clojure.set/union (:hash left-child) (:hash right-child)) nil)]
                                                    (doall (map
                                                            (partial update-parent rn)
                                                            [left-child right-child]))
                                                    (swap! range-nodes (fn [range-nodes] (assoc range-nodes (:hash rn) rn)))
                                                    rn
                                                    ))
                                                (take belt-range-count
                                                      (first (swap-vals! sorted-peaks (fn [current] (drop belt-range-count current)))))
                                                ))
                                      (map count (core/belt-ranges n))))
            belts (doall
                   (reduce (fn [left-child right-child]
                             (let [bn (belt-node (:hash left-child) (:hash right-child)
                                                 (clojure.set/union (:hash left-child) (:hash right-child)) nil)]
                               (doall (map
                                       (partial update-parent bn)
                                       [left-child right-child]))
                               (swap! belt-nodes (fn [belt-nodes] (assoc belt-nodes (:hash bn) bn)))
                               bn
                               ))
                           belt-children
                           ))
            ]
        {:belt-children belt-children
         :range-nodes range-nodes
         :belts belt-nodes
         :node-map node-map
         :node-array node-array
         }
        ))
    ))

(defonce result-1222-cached (play-algo-with-oneshot-nesting 1222 true))
(=
 (map #(if (instance? clojure.lang.Atom %) @% %) (vals result-1222-cached))
 (map #(if (instance? clojure.lang.Atom %) @% %) (vals (play-algo-with-oneshot-nesting 1222 true))))
(map some? (map (fn [val] (get @(:range-nodes result-1222) val))
                (map :hash (filter (fn [entry] (= :range (:type entry)))
                                   (map #(select-keys % [:type :hash]) (:belt-children result-1222))))))
;; -> can find all range nodes in collector

(map some? (map (fn [val] (get @(:node-map result-1222) val))
                (map :hash (filter (fn [entry] (= :range (:type entry)))
                                   ;; (map #(select-keys % [:type :hash]) (:belt-children result-1222) )))))
                                   (:belt-children result-1222)))))
;; -> can find first range node in collector

(map some? (map (fn [val] (:parent (get @(:range-nodes result-1222) val)))
                (map :hash (filter (fn [entry] (= :range (:type entry)))
                                   (map #(select-keys % [:type :hash]) (:belt-children result-1222))))))
;; ERGO -> the two last belt children don't have a daddy set, i.e. we're not updating this with final belt node? TODO: Investigate!!!

(count @(:range-nodes result-1222))
(count @(:belts result-1222))
;; ERGO -> it adds new range nodes, and the old ones don't attain parents!

(map some? (map (fn [val] (:parent (get @(:range-nodes result-1222) val)))
                (map :hash (filter (fn [entry] (= :range (:type entry)))
                                   (map #(select-keys % [:type :hash]) @(:range-nodes result-1222))))))
;; ERGO -> the two last belt children don't have a daddy set, i.e. we're not updating this with final belt node? TODO: Investigate!!!

(map some? (map (fn [val] (get @(:node-map result-1222) val))
      (map :hash (filter (fn [entry] (= :peak (:type entry))) (map #(select-keys % [:type :hash]) (:belt-children result-1222) )))))
;; -> peaks have parents set


(count @(:range-nodes result-1222))
(count @(:belts result-1222))
(get @(:range-nodes result-1222)
     (:hash (first (:belt-children result-1222))))

(map #(get (:node-map algo-1222)
           (nth (:node-array algo-1222) %))
     (map #(- % 3) (storage/parent-less-nodes 1222)))

(algo true)
(play-algo (last-algo-match) true)
(first-algo-mismatch)

(play-algo 300 false)

;; check that all peaks have correct children
(every? (fn [n]
          (eval `(and ~@((juxt
                          ;; number of leaves with this height
                          ;; (comp count second)
                          ;; does every leaf "hash" start with a 2^height
                          (comp (fn [list-first-indices] (every? #(= 0.0 %) (map #(mod % (Math/pow 2 n)) list-first-indices))) sort #(map first %) second)
                          ;; is every leaf "hash" a range from the first index until first index + 2^height?
                          (comp (fn [child-list] (every? #(= % (into #{} (range (first %) (+ (first %) (Math/pow 2 n))))) child-list)) second)
                          ;; identity
                          )
                         ;; group the peaks by hashset length
                         (nth (sort (group-by count (keys @node-map))) n)))))
        (range (count (core/S-n @leaf-count))))

(def algo-new-mismatch (play-algo (inc (last-algo-match)) true))
(def algo-old-mismatch (play-algo (inc (last-algo-match)) false))

(:mergeable-stack algo-new-mismatch)
(:mergeable-stack algo-old-mismatch)
(:lastP algo-new-mismatch)
(:lastP algo-old-mismatch)
(clojure.set/difference (into #{} (:node-map algo-new-mismatch))
                        (into #{} (:node-map algo-old-mismatch)))
(clojure.set/difference (into #{} (:node-map algo-old-mismatch))
                        (into #{} (:node-map algo-new-mismatch)))

(defn play-algo [n upgrade?]
  (do (reset-all)
      (doall (repeatedly n #(algo upgrade?)))
      ;; (println "-----------------")
      ;; (clojure.pprint/pprint @node-map)
      {:node-map @node-map
       :lastP @lastP
       :mergeable-stack @mergeable-stack
       :node-array @node-array}
      ))

;; test that that tree construction is correct
(let [n 1222
      nodes (play-algo n true)
      parent-less (filter #(= nil (:parent (val %))) (:node-map nodes))]
  (every? true?
          [
           (= (storage/S-n n) (reverse (sort (map (comp :height val) parent-less))))
           (= (storage/S-n n) (reverse (map (comp :height #(get @node-map %)) (take-while some? (iterate hop-left (:lastP nodes))))))
           (every? nil? (map #(:parent (get @node-map %)) (take-while #(some? (get @node-map %)) (iterate hop-left @lastP))))
           ]
          ))

(defn last-algo-match
  "plays algo while the upgrade and old algo still match"
  []
  (last (take-while
    #(let [
           non-upgrade (play-algo % false)
           upgrade (play-algo % true)
           ]
       (= non-upgrade upgrade))
    (range 300))))

(defn first-algo-mismatch
  "plays algo until first mismatch and returns the differences"
  []
  (let [first-mismatch (inc (last-algo-match))]
    (println "-----------------")
    (clojure.pprint/pprint
     {:first-mismatch first-mismatch
      :old (play-algo first-mismatch false)
      :new (play-algo first-mismatch true) })))

(do
  ;; (play-algo 10 false)
  (play-algo 0 false)
  (map (fn [[k v]] [k (:parent v)]) @node-map)
  (keys @node-map)
  )

(keys @node-map)
(get @node-map #{8 9})
