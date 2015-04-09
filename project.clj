(defproject sablono "0.3.5-SNAPSHOT"
  :description "Lisp style templating for Facebook's React."
  :url "http://github.com/r0man/sablono"
  :author "r0man"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cljsjs/react "0.12.2-5"]
                 [org.clojure/clojure "1.7.0-beta3"]
                 [org.clojure/clojurescript "0.0-3291" :scope "provided"]]
  :aliases {"cleantest" ["do" "clean," "test," "cljsbuild" "test"]
            "deploy" ["do" "clean," "deploy" "clojars"]}
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/dev/sablono.js"
                                   :output-dir "target/dev"
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true
                                   :verbose true}}
                       {:id "test"
                        :source-paths ["src" "test"]
                        :notify-command ["phantomjs" :cljs.test/runner "target/test/sablono.js"]
                        :compiler {:output-to "target/test/sablono.js"
                                   :output-dir "target/test"
                                   ;; :optimizations :advanced
                                   :optimizations :whitespace
                                   :pretty-print true
                                   :preamble ["jquery.js"
                                              "phantomjs-shims.js"]
                                   :externs ["externs/hickory.js"
                                             "externs/jquery-1.9.js"]
                                   :verbose true}}]
              :test-commands {"phantom" ["phantomjs" :cljs.test/runner "target/test/sablono.js"]}}
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev {:dependencies [[crate "0.2.5"]
                                  [com.cemerick/clojurescript.test "0.3.3"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [org.clojure/tools.reader "0.9.2"]
                                  [hickory "0.5.4"]
                                  [reagent "0.5.0"]]
                   :plugins [[com.cemerick/clojurescript.test "0.3.3"]
                             [lein-cljsbuild "1.0.6"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :resource-paths ["test-resources"]}})
