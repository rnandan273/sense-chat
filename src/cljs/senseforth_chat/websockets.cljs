(ns senseforth-chat.websockets
 (:require [clojure.walk :as walk]
 		   [cognitect.transit :as t]))

(defonce ws-chan (atom nil))
(def json-reader (t/reader :json))
(def json-writer (t/writer :json-verbose))

(defn receive-transit-msg!
 [update-fn]
 (fn [msg]
   (update-fn (->> msg .-data (t/read json-reader) (walk/keywordize-keys)))))

 (defn send-transit-msg!
 [token msg]
 ;(.log js/console "SENDING ->\n" (t/write json-writer {:token token :message msg}))
 (if @ws-chan
        (.send @ws-chan (t/write json-writer {:token token :message msg}))
   (throw (js/Error. "Websocket is not available!"))))

 (defn make-websocket! [url receive-handler]
 (println "attempting to connect websocket")
 (if-let [chan (js/WebSocket. url)]
   (do
     (set! (.-onmessage chan) (receive-transit-msg! receive-handler))
     (reset! ws-chan chan)
     (println "Websocket connection established with: " url))
   (throw (js/Error. "Websocket connection failed!"))))