(defproject senseforth-chat "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [selmer "1.0.0"]
                 [markdown-clj "0.9.86"]
                 [ring-middleware-format "0.7.0"]
                 [metosin/ring-http-response "0.6.5"]
                 [bouncer "1.0.0"]
                 [org.webjars/bootstrap "4.0.0-alpha.2"]
                 [org.webjars/font-awesome "4.5.0"]
                 [org.webjars.bower/tether "1.1.1"]
                 [org.webjars/jquery "2.2.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [com.taoensso/tower "3.0.2"]
                 [compojure "1.4.0"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.1.5"]
                 [ring-ttl-session "0.3.0"]
                 [ring "1.4.0" :exclusions [ring/ring-jetty-adapter]]
                 [mount "0.1.10"]
                 [cprop "0.1.6"]
                 [org.clojure/tools.cli "0.3.3"]
                 [luminus-nrepl "0.1.4"]
                 [luminus-http-kit "0.1.3"]
                 [org.clojure/clojurescript "1.7.228" :scope "provided"]
                 [reagent "0.5.1"]
                 [reagent-forms "0.5.20"]
                 [reagent-utils "0.1.7"]
                 [secretary "1.2.3"]
                 [org.clojure/core.async "0.2.374"]
                 [cljs-ajax "0.5.3"]
                 [com.novemberain/langohr "3.5.0"]
                 [clojurewerkz/elastisch "2.2.1"]
                 [luminus-log4j "0.1.3"]]

  :min-lein-version "2.0.0"

  :jvm-opts ["-server" "-Dconf=.lein-env"]
  :source-paths ["src/clj" "src/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]

  :main senseforth-chat.core

  :plugins [[lein-cprop "1.0.1"]
            [lein-cljsbuild "1.1.1"]]
  :clean-targets ^{:protect false} [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :cljsbuild
  {:builds
   {:app
    {:source-paths ["src/cljc" "src/cljs"]
     :compiler
     {:output-to "target/cljsbuild/public/js/app.js"
      :output-dir "target/cljsbuild/public/js/out"
      :externs ["react/externs/react.js"]
      :pretty-print true}}}}
  
  :profiles
  {:uberjar {:omit-source true
             
              :prep-tasks ["compile" ["cljsbuild" "once"]]
              :cljsbuild
              {:builds
               {:app
                {:source-paths ["env/prod/cljs"]
                 :compiler
                 {:optimizations :advanced
                  :pretty-print false
                  :closure-warnings
                  {:externs-validation :off :non-standard-jsdoc :off}}}}} 
             
             :aot :all
             :uberjar-name "senseforth-chat.jar"
             :source-paths ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}
   :dev           [:project/dev :profiles/dev]
   :test          [:project/test :profiles/test]
   :project/dev  {:dependencies [[prone "1.0.2"]
                                 [ring/ring-mock "0.3.0"]
                                 [ring/ring-devel "1.4.0"]
                                 [pjstadig/humane-test-output "0.7.1"]
                                 [lein-figwheel "0.5.0-6"]
                                 [lein-doo "0.1.6"]
                                 [com.cemerick/piggieback "0.2.2-SNAPSHOT"]]
                  :plugins [[lein-figwheel "0.5.0-6"] [lein-doo "0.1.6"] [org.clojure/clojurescript "1.7.228"]]
                   :cljsbuild
                   {:builds
                    {:app
                     {:source-paths ["env/dev/cljs"]
                      :compiler
                      {:main "senseforth-chat.app"
                       :asset-path "/js/out"
                       :optimizations :none
                       :source-map true}}
                     :test
                     {:source-paths ["src/cljc" "src/cljs" "test/cljs"]
                      :compiler
                      {:output-to "target/test.js"
                       :main "senseforth-chat.doo-runner"
                       :optimizations :whitespace
                       :pretty-print true}}}} 
                  
                  :figwheel
                  {:http-server-root "public"
                   :server-port 3449
                   :nrepl-port 7002
                   :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
                   :css-dirs ["resources/public/css"]
                   :ring-handler senseforth-chat.handler/app}
                  :doo {:build "test"}
                  :source-paths ["env/dev/clj" "test/clj"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:resource-paths ["env/dev/resources" "env/test/resources"]}
   :profiles/dev {}
   :profiles/test {}})
