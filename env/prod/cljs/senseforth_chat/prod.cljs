(ns senseforth-chat.app
  (:require [senseforth-chat.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
