(ns senseforth-chat.core
  (:require [reagent.core :as r]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [cognitect.transit :as t]
            [clojure.walk :as walk]
            [senseforth-chat.websockets :as ws]
            [reagent.core :as reagent :refer [atom]]
            [senseforth-chat.ajax :refer [load-interceptors!]]
            [ajax.core :refer [GET POST]])
  (:import goog.History))

(defonce messages (atom []))
(def unique-token (atom -1))
(def json-reader (t/reader :json))
(def json-writer (t/writer :json))

(defn update-messages! [message]
;(defn update-messages! [{:keys [message]}]
  ;(.log js/console (str "in update-messages MESSAGE " (walk/keywordize-keys message)))
  (.log js/console (str "in update-messages MESSAGE " (:message message) (:token message)))
  (reset! unique-token (:token message))
  (swap! messages #(vec (take 10 (conj % (:message message))))))

(defn message-list []
 [:ul
  (for [[i message] (map-indexed vector @messages)]
    ^{:key i}
    [:li message])])

(defn message-input []
 (let [value (atom nil)]
   (fn []
     [:input.form-control
      {:type :text
       :placeholder "type in a message and press enter"
       :value @value
       :on-change #(reset! value (-> % .-target .-value))
       :on-key-down
       #(when (= (.-keyCode %) 13)
          ;(.log js/console {:message @value})
          (ws/send-transit-msg!
           (str @unique-token) @value)
          (reset! value nil))}])))

(defn nav-link [uri title page collapsed?]
  [:ul.nav.navbar-nav>a.navbar-brand
   {:class (when (= page (session/get :page)) "active")
    :href uri
    :on-click #(reset! collapsed? true)}
   title])

(defn navbar []
  (let [collapsed? (r/atom true)]
    (fn []
      [:nav.navbar.navbar-light.bg-faded
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:a.navbar-brand {:href "#/"} "senseforth-chat"]
        [:ul.nav.navbar-nav
         [nav-link "#/" "Home" :home collapsed?]
         [nav-link "#/about" "About" :about collapsed?]]]])))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "this is the story of senseforth-chat... work in progress"]]])

(defn home-page []
  [:div.container
   [:div.jumbotron
    [:h1 "Welcome to senseforth chat"]]
    [:div.row
   [:div.col-sm-6
    [message-list]]]
  [:div.row
   [:div.col-sm-6
    [message-input]]]])

(defn home-page-old []
  [:div.container
   [:div.jumbotron
    [:h1 "Welcome to senseforth chat"]
    [:p "Time to start building your site!"]
    [:p [:a.btn.btn-primary.btn-lg {:href "http://luminusweb.net"} "Learn more »"]]]
   [:div.row
    [:div.col-md-12
     [:h2 "Welcome to ClojureScript"]]]
    [:div.row
   [:div.col-sm-6
    [message-list]]]
  [:div.row
   [:div.col-sm-6
    [message-input]]]
   (when-let [docs (session/get :docs)]
     [:div.row
      [:div.col-md-12
       [:div {:dangerouslySetInnerHTML
              {:__html (md->html docs)}}]]])])

(def pages
  {:home #'home-page
   :about #'about-page})

(defn page []
  [(pages (session/get :page))])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :page :home))

(secretary/defroute "/about" []
  (session/put! :page :about))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
        (events/listen
          HistoryEventType/NAVIGATE
          (fn [event]
              (secretary/dispatch! (.-token event))))
        (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET (str js/context "/docs") {:handler #(session/put! :docs %)}))

(defn mount-components []
  (r/render [#'navbar] (.getElementById js/document "navbar"))
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (.log js/console (str "ws://" (.-host js/location) "/ws"))
  (load-interceptors!)
  (ws/make-websocket! (str "ws://" (.-host js/location) "/ws") update-messages!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
