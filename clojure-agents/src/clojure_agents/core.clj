(ns clojure-agents.core
   (:require [clj-http.client :as http-client])
   (:require [clojure.data.json :as json]))

(def start (System/nanoTime))
(def graph (agent {}))
(def graph-builder (agent {:edges-assembled 0 :data {}}))
(def edges-amount (agent 0))

(defn- build-graph [_ builder]
  (let [flattened-adjacencies 
        (apply concat 
               (for [entry (builder :data)] 
                 (for [target-weight (entry 1)] 
                   (cons (entry 0) target-weight))))]
    (assoc {} :adjacencies flattened-adjacencies :vertices (count (builder :data)))))

(defn- on-graph-built [key ref old graph] 
  (do 
    (prn (format "Vertices: %d" (graph :vertices)))
    (prn (format "Edges: %d" (count (graph :adjacencies))))
    (prn (format "Milliseconds taken: %d" (Math/round (double (/ (- (System/nanoTime) start) 1000000)))))))

(defn- put-on-builder
  [builder partial-edges]
  (let [partial-map 
        (reduce #(assoc-in %1 [(:i %2) (:j %2)] (:weight %2)) {} partial-edges)]
    (assoc builder 
           :edges-assembled (+ (builder :edges-assembled) (count partial-edges))
           :data (merge-with merge (builder :data) partial-map))))

(defn- on-builder-updated [key ref old builder] 
  (if (= @edges-amount (:edges-assembled builder))
    (send graph build-graph builder)))


(defn- get-edges
  [_ offset limit]
  (json/read-str 
    (:body (http-client/get (format "http://localhost:8080/api/graph?offset=%d&limit=%d" 
                                    offset limit)))
    :key-fn keyword))

(defn- assemble-graph
  [assembled partial-edges]
  (merge-with merge assembled partial-edges))

(defn- on-edges-received
  [key ref old edges]
  (send graph-builder put-on-builder edges))

(defn- get-graph
  [offsets]
  (doseq [offset offsets]
    (let [edges (agent [])]
      (do
        (add-watch edges :edges-watcher on-edges-received)
        (set-error-mode! edges :continue)
        (set-error-handler! 
          edges
          (fn [agent _] (send-off agent get-edges offset 1000)))
        (send-off edges get-edges offset 1000)))))

(defn- edges-amount-watcher
  [key ref old edges-amount]
  (def offsets 
    (map #(* 1000 %) 
         (let [batches-amount 
               (+ (quot edges-amount 1000) (Integer/signum (mod edges-amount 1000)))] 
           (range batches-amount))))
  (get-graph offsets))

(defn main
  []
  (add-watch graph :on-graph-built on-graph-built)
  (add-watch graph-builder :on-builder-updated on-builder-updated)
  (add-watch edges-amount :edges-amount-watcher edges-amount-watcher)
  (send-off edges-amount 
            (fn [_] (Integer/parseInt 
                      (:body (http-client/get "http://localhost:8080/api/graph/edges-quantity"))))))
