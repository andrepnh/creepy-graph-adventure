(ns clojure-agents.core
   (:require [clj-http.client :as http-client]))

(defn main
  []
  (def edges-amount (agent 0))
  (add-watch edges-amount :edges-amount-watcher 
             (fn [_ ref old new] (println old new)))
  (send-off edges-amount (
    fn [_] (Integer/parseInt (:body (http-client/get "http://localhost:8080/api/graph/edges-quantity")))))
  (Thread/sleep 3000)
  (shutdown-agents))
