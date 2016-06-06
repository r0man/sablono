(defproject sablono "0.7.3-SNAPSHOT"
  :description "Lisp style templating for Facebook's React."
  :url "http://github.com/r0man/sablono"
  :author "r0man"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:dev {:dependencies [[cljsjs/jquery "2.2.2-0"]
                                  [crate "0.2.5"]
                                  [criterium "0.4.4"]
                                  [devcards "0.2.1-7" :exclusions [sablono]]
                                  [doo "0.1.6"]
                                  [figwheel-sidecar "0.5.3-2"]
                                  [hickory "0.6.0"]
                                  [reagent "0.6.0-alpha2"]
                                  [rum "0.9.0"]]
                   :plugins [[lein-cljsbuild "1.1.3"]
                             [lein-doo "0.1.6"]
                             [lein-figwheel "0.5.2"]]
                   :resource-paths ["test-resources" "target"]}
             :provided {:dependencies [[cljsjs/react "15.1.0-0"]
                                       [cljsjs/react-dom "15.1.0-0"]
                                       [cljsjs/react-dom-server "15.1.0-0"]
                                       [org.clojure/clojurescript "1.9.36"]]}
             :repl {:dependencies [[com.cemerick/piggieback "0.2.1"]]
                    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  :aliases {"ci" ["do"
                  ["clean"]
                  ["test" ":default"]
                  ["doo" "phantom" "none" "once"]
                  ["doo" "phantom" "advanced" "once"]]
            "deploy" ["do" "clean," "deploy" "clojars"]}
  :clean-targets ^{:protect false} [:target-path]
  :cljsbuild {:builds
              [{:id "devcards"
                :compiler
                {:asset-path "devcards"
                 :main sablono.test
                 :output-to "target/public/sablono.js"
                 :output-dir "target/public/devcards"
                 :optimizations :none
                 :pretty-print true
                 :source-map true
                 :verbose false}
                :figwheel {:devcards true}
                :source-paths ["src" "test"]}
               {:id "none"
                :compiler
                {:asset-path "target/none/out"
                 :main sablono.test
                 :output-to "target/none/sablono.js"
                 :output-dir "target/none/out"
                 :optimizations :none
                 :pretty-print true
                 :source-map true
                 :verbose false}
                :source-paths ["src" "test"]}
               {:id "advanced"
                :compiler
                {:asset-path "target/advanced/out"
                 :main sablono.test
                 :output-to "target/advanced/sablono.js"
                 :optimizations :advanced
                 :pretty-print true
                 :verbose false}
                :source-paths ["src" "test"]}]}
  :deploy-repositories [["releases" :clojars]]
  :test-selectors {:benchmark :benchmark
                   :default (complement :benchmark)})
