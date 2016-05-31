(ns senseforth-chat.routes.es_utils
  (:require [clojurewerkz.elastisch.rest          :as esr]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.rest.index    :as esi]
            [clojurewerkz.elastisch.query         :as q]
            [clojurewerkz.elastisch.aggregation   :as a]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [taoensso.timbre :as timbre]
            [clojure.pprint :as pp])
  
  (:gen-class))

(def conn (esr/connect "http://127.0.0.1:9200"))

(defn escreate-index []
  (let [conn          (esr/connect "http://127.0.0.1:9200")
        mapping-types {:chat {:properties {:author   {:type "string"}
                                             :content {:type "string"}
                                             :date  {:type "date"}
                                             :responded       {:type "boolean"}
                                             :tags      {:type "string"}
                                             :response     {:type "string"}}}}]
    (esi/create conn "senseforth" :mappings mapping-types)))

(defn esagg []
  (let [conn (esr/connect "http://127.0.0.1:9200")
        res (esd/search conn "senseforth" "chat" {:query (q/term :msg "testing message")
                                                    :aggregations {:age (a/avg "age")}})]
  (esrsp/aggregation-from res)))

(defn esadd [message token status id]
  (let [conn (esr/connect "http://127.0.0.1:9200")
        doc {:msg message :token token :status status}]
  ;; index a document
  (esd/put conn "senseforth" "chat" id doc)))

(defn escreate [request-msg response-msg token resp-status tag]
  
  (let [;conn (esr/connect "http://127.0.0.1:9200")
        doc {:content request-msg :response response-msg :author (str token) :responded resp-status :tags tag :date (System/currentTimeMillis)}
        ;doc {:msg message :token token :status status}
        ]
  ;; index a document
  (timbre/info "Publishing to ES >>>>>>>>>>>>>>\n" doc)
  ;(esd/create conn "senseforth" "chat" doc))
  ))

(defn esquery [token]
  (let [conn (esr/connect "http://127.0.0.1:9200")
        res  (esd/search conn "senseforth" "chat"
                         :query (q/term :token token)
                         :sort  {:msg "desc"})
        hits (esrsp/hits-from res)]
    (pp/pprint hits)))