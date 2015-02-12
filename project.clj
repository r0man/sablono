(defproject sablono "0.3.2-SNAPSHOT"
  :description "Lisp style templating for Facebook's React."
  :url "http://github.com/r0man/sablono"
  :author "r0man"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cljsjs/react "0.12.2-5"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2816" :scope "provided"]]
  :aliases {"cleantest" ["do" "clean," "cljx" "once," "test," "cljsbuild" "test"]
            "deploy" ["do" "clean," "cljx" "once," "deploy" "clojars"]}
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["test" "target/classes" "target/test-classes"]
                        :compiler {:output-to "target/dev/sablono.js"
                                   :output-dir "target/dev"
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true}}
                       {:id "test"
                        :source-paths ["test" "target/classes" "target/test-classes"]
                        :notify-command ["phantomjs" :cljs.test/runner "target/test/sablono.js"]
                        :compiler {:output-to "target/test/sablono.js"
                                   :output-dir "target/test"
                                   :optimizations :advanced
                                   ;; :optimizations :whitespace
                                   :pretty-print true
                                   :preamble ["jquery.js"
                                              "phantomjs-shims.js"]
                                   :externs ["externs/hickory.js"
                                             "externs/jquery-1.9.js"]}}]
              :test-commands {"phantom" ["phantomjs" :cljs.test/runner "target/test/sablono.js"]}}
  :cljx {:builds [{:source-paths ["src"]
                   :output-path "target/classes"
                   :rules :clj}
                  {:source-paths ["src"]
                   :output-path "target/classes"
                   :rules :cljs}
                  {:source-paths ["test"]
                   :output-path "target/test-classes"
                   :rules :clj}
                  {:source-paths ["test"]
                   :output-path "target/test-classes"
                   :rules :cljs}]}
  :deploy-repositories [["releases" :clojars]]
  :prep-tasks [["cljx" "once"]]
  :profiles {:dev {:dependencies [[crate "0.2.5"]
                                  [com.cemerick/piggieback "0.1.6-SNAPSHOT"]
                                  [hickory "0.5.4"]
                                  [org.clojure/tools.nrepl "0.2.7"]
                                  [reagent "0.4.3"]]
                   :plugins [[com.keminglabs/cljx "0.5.0"]
                             [com.cemerick/clojurescript.test "0.3.3"]
                             [lein-cljsbuild "1.0.4"]]
                   :repl-options {:nrepl-middleware [cljx.repl-middleware/wrap-cljx]}
                   :resource-paths ["test-resources"]
                   :source-paths ["target/classes"]
                   :test-paths ["test" "target/test-classes"]}})
