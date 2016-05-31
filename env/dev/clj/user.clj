(ns user
  (:require [mount.core :as mount]
            senseforth-chat.core))

(defn start []
  (mount/start-without #'senseforth-chat.core/repl-server))

(defn stop []
  (mount/stop-except #'senseforth-chat.core/repl-server))

(defn restart []
  (stop)
  (start))


