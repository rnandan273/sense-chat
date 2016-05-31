(ns senseforth-chat.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[senseforth-chat started successfully]=-"))
   :middleware identity})
