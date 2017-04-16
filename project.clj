(defproject sablono "0.8.1-SNAPSHOT"
  :description "Lisp style templating for Facebook's React."
  :url "http://github.com/r0man/sablono"
  :author "r0man"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]]
  :profiles {:dev {:dependencies [[cljsjs/jquery "2.2.4-0"]
                                  [crate "0.2.5"]
                                  [criterium "0.4.4"]
                                  [devcards "0.2.3" :exclusions [sablono]]
                                  [doo "0.1.7"]
                                  [figwheel-sidecar "0.5.10"]
                                  [funcool/tubax "0.2.0"]
                                  [org.clojure/test.check "0.9.0"]
                                  [reagent "0.6.1"]
                                  [rum "0.10.8" :exclusions [sablono]]]
                   :plugins [[lein-cljsbuild "1.1.4"]
                             [lein-doo "0.1.7"]
                             [lein-figwheel "0.5.8"]]
                   :resource-paths ["test-resources" "target"]}
             :provided {:dependencies [[cljsjs/react "15.5.0-0"]
                                       [cljsjs/react-dom "15.5.0-0"]
                                       [cljsjs/react-dom-server "15.5.0-0"]
                                       [org.clojure/clojurescript "1.9.521"]]}
             :repl {:dependencies [[com.cemerick/piggieback "0.2.1"]]
                    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}}
  :aliases {"ci" ["do"
                  ["clean"]
                  ["test" ":default"]
                  ["doo" "node" "nodejs" "once"]
                  ;; TODO: Fix ReferenceError: Can't find variable: React
                  ;; ["doo" "phantom" "none" "once"]
                  ["doo" "nashorn" "advanced" "once"]
                  ["doo" "phantom" "advanced" "once"]
                  ["doo" "phantom" "benchmark" "once"]]
            "deploy" ["do" "clean," "deploy" "clojars"]}
  :clean-targets ^{:protect false} [:target-path]
  :cljsbuild {:builds
              [{:id "benchmark"
                :compiler
                {:asset-path "target/benchmark/out"
                 :main sablono.benchmark
                 :output-dir "target/benchmark/out"
                 :output-to "target/benchmark/sablono.js"
                 :optimizations :advanced
                 :pretty-print true
                 :verbose false}
                :source-paths ["src" "benchmark"]}
               {:id "devcards"
                :compiler
                {:asset-path "devcards"
                 :main sablono.test.runner
                 :output-to "target/public/sablono.js"
                 :output-dir "target/public/devcards"
                 :optimizations :none
                 :pretty-print true
                 :source-map true
                 :verbose false}
                :figwheel {:devcards true}
                :source-paths ["src" "test"]}
               {:id "nodejs"
                :compiler
                {:asset-path "target/nodejs/out"
                 :main sablono.test.runner
                 :optimizations :none
                 :output-dir "target/nodejs/out"
                 :output-to "target/nodejs/sablono.js"
                 :pretty-print true
                 :source-map true
                 :target :nodejs
                 :verbose false}
                :source-paths ["src" "test"]}
               {:id "none"
                :compiler
                {:asset-path "target/none/out"
                 :main sablono.test.runner
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
                 :main sablono.test.runner
                 :output-dir "target/advanced/out"
                 :output-to "target/advanced/sablono.js"
                 :optimizations :advanced
                 :pretty-print true
                 :verbose false}
                :source-paths ["src" "test"]}]}
  :deploy-repositories [["releases" :clojars]]
  :test-selectors {:benchmark :benchmark
                   :default (complement :benchmark)})
