(ns senseforth-chat.routes.websockets
 (:gen-class)
 (:require [compojure.core :refer [GET defroutes]]
           [org.httpkit.server
            :refer [send! with-channel on-close on-receive]]
           [cognitect.transit :as t]
           [clojure.string :as cstr]
           [taoensso.timbre :as timbre]
           [langohr.core      :as rmq]
           [langohr.channel   :as lch]
           [langohr.exchange  :as le]
           [langohr.queue     :as lq]
           [langohr.consumers :as lc]
           [langohr.basic     :as lb]
           [clojure.data.json :as json]
           [senseforth-chat.routes.es_utils :as eu])
 (:use [clojure.string :only (join split)]))

(def ^{:const true}
  default-exchange-name "")

(defonce channels (atom []))

(defonce privateqs (atom []))

(defonce userqs (atom {}))

(defonce ch (lch/open (rmq/connect)))

(defn connect! [channel]
 (timbre/info "channel open")
	(swap! channels conj channel))

(defn disconnect! [channel status]
 (timbre/info "channel closed:" status)
 (swap! channels #(remove #{channel} %)))

(defn get-user-token [token-tag]
  (let [token-tag (Integer/parseInt token-tag)]
  (if (= -1 token-tag)
      (rand-int 10000000000)
      token-tag)))

(defn publish-es [raw-msg]
  (timbre/info "PUBLISH-ES " raw-msg)
  (let [json-es-data (json/read-str raw-msg :key-fn keyword)
        stage  (:type json-es-data)
        token  (:token json-es-data)
        msg  (:message json-es-data)
        pipe-index (cstr/index-of msg "|")
        clean-msg-index (cstr/index-of msg " ")
        ]

   (if (= nil pipe-index)
       (eu/escreate (subs msg (inc clean-msg-index)) "nil" token false stage)
       (eu/escreate (subs msg (inc clean-msg-index) pipe-index) (subs msg (inc pipe-index)) token true stage))))

(defn ws-handler [request]
 (with-channel request channel
               (on-close channel (partial disconnect! channel))
               (on-receive channel (fn [data]
               (timbre/info "Received - " data)
               (let [valsMap (vals (json/read-str data))
                      user-tok  (get-user-token (nth valsMap 0))
                      msg  (str user-tok " " (nth valsMap 1))
      				        req-qname "langohr.senseforth.chatreq"
                      es-qname "langohr.senseforth.es"
      				        resp-qname "langohr.senseforth.chatresp"]
				   		(timbre/info "Userqs " (count @userqs))
              (timbre/info "msg -> " msg)
              (timbre/info "Token -> " user-tok)

				   		(lq/declare ch req-qname {:exclusive false :durable true :auto-delete false})
              (lq/declare ch es-qname {:exclusive false :durable true :auto-delete false})
              ;(lb/publish ch default-exchange-name es-qname (str "req" ":" user-tok ":" msg))
              (lb/publish ch default-exchange-name es-qname (json/write-str {:type "req" :token user-tok :message msg}))

              (if (contains? @userqs (keyword user-tok))
				   			   (timbre/info "Queue present" ((keyword user-tok) @userqs))
				   		     (let [
							        queue-name (lq/declare-server-named ch {:exclusive true :auto-delete true})

							        handler    (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
                                   (let [raw-msg (String. payload "UTF-8")
                                         resp-msg (subs raw-msg (inc (cstr/index-of raw-msg "," (inc (cstr/index-of raw-msg ",")))) (count raw-msg));;(get (split raw-msg #",") 2)
                                         token (subs resp-msg 1 (cstr/index-of resp-msg " "))]
                                        ;(lb/publish ch default-exchange-name es-qname (str "resp" ":" token ":" resp-msg))
                                        (lb/publish ch default-exchange-name es-qname (json/write-str {:type "resp" :token token :message resp-msg}))
							                          (send! channel (json/write-str {:token token :message resp-msg}))
                                        ))

							        resp-handler (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
                   								   (timbre/info "Received on server response queue" (String. payload "UTF-8"))
                   								   (let [json-resp-data (json/read-str (String. payload "UTF-8") :key-fn keyword)
                                           client-qname  (:userqueue json-resp-data)
                                           client-req-msg (:request json-resp-data)
                                           client-resp-msg (:response json-resp-data)
                                           client-token (:token json-resp-data)
                                           client-msg (:response json-resp-data)
                                           resp-modified (str "[\"^ \",\"~:message\"," (str "\"" client-token " " client-req-msg "|" client-resp-msg "\""))
                                           ]

                                           (timbre/info "MODIFIED MSG \n")
                                           (timbre/info client-qname)
                                           (timbre/info client-req-msg)
                                           (timbre/info client-resp-msg)
                                           (timbre/info client-token)
                                           (timbre/info resp-modified)
                                           (timbre/info ">>>>>>\n\n")
                                           (lb/publish ch default-exchange-name client-qname resp-modified)))

                      es-handler (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
                                     (timbre/info "Received on ES queue" (String. payload "UTF-8"))
                                     (publish-es (String. payload "UTF-8"))
                                     )]
							        ;; publish to server request queue
    								(lc/subscribe ch queue-name handler {:auto-ack true})
    								(swap! userqs assoc-in [(keyword (str user-tok))] queue-name)
                    (lc/subscribe ch resp-qname resp-handler {:auto-ack true})
                    (lc/subscribe ch es-qname es-handler {:auto-ack true})
  									;; till here

    								))
              ;; publish to the request queue
              (timbre/info "Request message ->" (json/write-str {:userqueue ((keyword (str user-tok)) @userqs) :token user-tok :request (nth valsMap 1)}))
    					;(lb/publish ch default-exchange-name req-qname (str ((keyword (str user-tok)) @userqs) ":" (str "\"" msg "\""))) 
              (lb/publish ch default-exchange-name req-qname (json/write-str {:userqueue ((keyword (str user-tok)) @userqs) :token (str user-tok) :request (nth valsMap 1)}))
                                       )))))


(defroutes websocket-routes
 (GET "/ws" request (ws-handler request)))


