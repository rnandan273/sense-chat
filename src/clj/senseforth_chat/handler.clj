(ns senseforth-chat.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [senseforth-chat.layout :refer [error-page]]
            [senseforth-chat.routes.home :refer [home-routes]]
            [compojure.route :as route]
            [senseforth-chat.middleware :as middleware]
            [senseforth-chat.routes.websockets :as ws]))

(def app-routes
  (routes
    ws/websocket-routes
    (wrap-routes #'home-routes middleware/wrap-csrf)
    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))

(def app (middleware/wrap-base #'app-routes))
