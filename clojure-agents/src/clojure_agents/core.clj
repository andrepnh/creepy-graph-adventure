(ns clojure-agents.core
   (:require [clj-http.client :as http-client])
   (:require [clojure.data.json :as json]))

(defn- get-edges
  [offset limit]
  (json/read-str 
    (:body (http-client/get (format "http://localhost:8080/api/graph?offset=%d&limit=%d" 
                                    offset limit)))
    :key-fn keyword))

(defn- get-graph
  [offsets]
  (letfn [(collect-into-builder [builder edges] 
            (reduce #(assoc-in %1 [(:i %2) (:j %2)] (:weight %2)) {} edges))] 
          (reduce collect-into-builder 
            {}
            (map #(get-edges % 1000) offsets))))

(defn- edges-amount-watcher
  [key ref old edges-amount]
  (def offsets 
    (map #(* 1000 %) 
         (let [batches-amount 
               (+ (quot edges-amount 1000) (Integer/signum (mod edges-amount 1000)))] 
           (range batches-amount))))
  (println (get-graph offsets)))

(defn main
  []
  (def edges-amount (agent 0))
  (add-watch edges-amount :edges-amount-watcher edges-amount-watcher)
  (send-off edges-amount 
            (fn [_] (Integer/parseInt 
                      (:body (http-client/get "http://localhost:8080/api/graph/edges-quantity"))))))
