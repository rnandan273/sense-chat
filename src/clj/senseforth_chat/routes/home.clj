(ns senseforth-chat.routes.home
  (:gen-class)
  (:require [senseforth-chat.layout :as layout]
            [compojure.core :refer [defroutes GET]]
            [ring.util.http-response :as response]
            [clojure.java.io :as io]
            [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.exchange  :as le]
            [langohr.queue     :as lq]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]))

(def ^{:const true}
  default-exchange-name "")


(def ^{:const true}
  weather-exchange "weathr")

(defn message-handler
  [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (println (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") delivery-tag content-type type)))

(defn chat-message-handler
  [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (println (format "[consumer] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                   (String. payload "UTF-8") delivery-tag content-type type)))

(defn senseforth-twitter-consumer
  "Starts a consumer bound to the given topic exchange in a separate thread"
  [ch topic-name username qname]
  (let [queue-name (lq/declare-server-named ch {:exclusive true :auto-delete true})
        handler    (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
                     (println (format "[consumer xxx] %s received %s" username (String. payload "UTF-8")))
                     ;(lb/publish ch default-exchange-name qname "Hello!" {:content-type "text/plain" :type "greetings.hi"})
                     )]
    ;(lq/declare ch queue-name {:exclusive false :auto-delete true})
    (lq/bind    ch queue-name topic-name)
    (lc/subscribe ch queue-name handler {:auto-ack true})
    ;(lb/publish ch default-exchange-name qname "Hello!" {:content-type "text/plain" :type "greetings.hi"})
))

(defn senseforth-twitter-example
  []
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        ex    "nba.scores"
        qname "langohr.senseforth.chat"
        users ["joe" "aaron" "bob"]]
    (le/declare ch ex "fanout" {:durable false :auto-delete true})
    (doseq [u users]
      (senseforth-twitter-consumer ch qname u))

    ;(lb/publish ch ex "" "BOS 101, NYK 89" {:content-type "text/plain" :type "scores.update"})
    ;(lb/publish ch ex "" "ORL 85, ALT 88"  {:content-type "text/plain" :type "scores.update"})

    (lq/declare ch qname {:exclusive false :auto-delete true})
    (lc/subscribe ch qname chat-message-handler {:auto-ack true})
    (Thread/sleep 2000)
    (rmq/close ch)
    (rmq/close conn)))


(defn senseforth-chat-ex []
  (let [conn  (rmq/connect)
      ch    (lch/open conn)
      queue (lq/declare-server-named ch {:exclusive true :auto-delete true})]
  (comment 
  (lc/subscribe ch qname message-handler {:auto-ack true})
   (lb/publish ch default-exchange-name qname "Hello!" {:content-type "text/plain" :type "greetings.hi"})
   )
  (Thread/sleep 2000)
    (println "[main] Disconnecting...")
    (rmq/close ch)
    (rmq/close conn)))

(defn chat-example []
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        qname "langohr.senseforth.chat"]
    (println (format "[main] Connected. Channel id: %d" (.getChannelNumber ch)))
    (lq/declare ch qname {:exclusive false :auto-delete true})
    (lc/subscribe ch qname message-handler {:auto-ack true})
    (lb/publish ch default-exchange-name qname "Hello!" {:content-type "text/plain" :type "greetings.hi"})
    (Thread/sleep 2000)
    (println "[main] Disconnecting...")
    (rmq/close ch)
    (rmq/close conn)))

(defn start-twitter-consumer
  "Starts a consumer bound to the given topic exchange in a separate thread"
  [ch topic-name username]
  (let [queue-name (format "nba.newsfeeds.%s" username)
        handler    (fn [ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
                     (println (format "[consumer] %s received %s" username (String. payload "UTF-8"))))]
    (lq/declare ch queue-name {:exclusive false :auto-delete true})
    (lq/bind    ch queue-name topic-name)
    (lc/subscribe ch queue-name handler {:auto-ack true})))

(defn twitter-example
  []
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        ex    "nba.scores"
        users ["joe" "aaron" "bob"]]
    (le/declare ch ex "fanout" {:durable false :auto-delete true})
    (doseq [u users]
      (start-twitter-consumer ch ex u))
    (lb/publish ch ex "" "BOS 101, NYK 89" {:content-type "text/plain" :type "scores.update"})
    (lb/publish ch ex "" "ORL 85, ALT 88"  {:content-type "text/plain" :type "scores.update"})
    (Thread/sleep 2000)
    (rmq/close ch)
    (rmq/close conn)))

(defn start-weather-consumer
  "Starts a consumer bound to the given topic exchange in a separate thread"
  [ch topic-name queue-name]
  (let [queue-name' (:queue (lq/declare ch queue-name {:exclusive false :auto-delete true}))
        handler     (fn [ch {:keys [routing-key] :as meta} ^bytes payload]
                      (println (format "[consumer] Consumed '%s' from %s, routing key: %s" (String. payload "UTF-8") queue-name' routing-key)))]
    (lq/bind    ch queue-name' weather-exchange {:routing-key topic-name})
    (lc/subscribe ch queue-name' handler {:auto-ack true})))

(defn publish-update
  "Publishes a weather update"
  [ch payload routing-key]
  (lb/publish ch weather-exchange routing-key payload {:content-type "text/plain" :type "weather.update"}))

(defn weather-example
  [& args]
  (let [conn      (rmq/connect)
        ch        (lch/open conn)
        locations {""               "americas.north.#"
                   "americas.south" "americas.south.#"
                   "us.california"  "americas.north.us.ca.*"
                   "us.tx.austin"   "#.tx.austin"
                   "it.rome"        "europe.italy.rome"
                   "asia.hk"        "asia.southeast.hk.#"}]
    (le/declare ch weather-exchange "topic" {:durable false :auto-delete true})
    (doseq [[k v] locations]
      (start-weather-consumer ch v k))
    (publish-update ch "San Diego update" "americas.north.us.ca.sandiego")
    (publish-update ch "Berkeley update"  "americas.north.us.ca.berkeley")
    (publish-update ch "SF update"        "americas.north.us.ca.sanfrancisco")
    (publish-update ch "NYC update"       "americas.north.us.ny.newyork")
    (publish-update ch "São Paolo update" "americas.south.brazil.saopaolo")
    (publish-update ch "Hong Kong update" "asia.southeast.hk.hongkong")
    (publish-update ch "Kyoto update"     "asia.southeast.japan.kyoto")
    (publish-update ch "Shanghai update"  "asia.southeast.prc.shanghai")
    (publish-update ch "Rome update"      "europe.italy.roma")
    (publish-update ch "Paris update"     "europe.france.paris")
    (Thread/sleep 2000)
    (rmq/close ch)
    (rmq/close conn)))

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/docs" [] (response/ok (-> "docs/docs.md" io/resource slurp))))

