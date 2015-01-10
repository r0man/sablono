(defproject sablono "0.3.0-SNAPSHOT"
  :description "Lisp style templating for Facebook's React."
  :url "http://github.com/r0man/sablono"
  :author "r0man"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.facebook/react "0.12.2.1"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2665" :scope "provided"]]
  :aliases {"cleantest" ["do" "clean," "cljx" "once," "test," "cljsbuild" "test"]}
  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["test" "target/classes" "target/test-classes"]
                        :compiler {:output-to "target/dev/sablono.js"
                                   :output-dir "target/dev"
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true}}
                       {:id "test"
                        :source-paths ["test" "target/classes" "target/test-classes"]
                        :compiler {:output-to "target/test/sablono.js"
                                   :output-dir "target/test"
                                   :optimizations :advanced
                                   :pretty-print true
                                   :preamble ["jquery.js"
                                              "phantomjs-shims.js"
                                              "react/react.min.js"]
                                   :externs ["externs/jquery-1.9.js"]}}]
              :test-commands {"phantom" ["phantomjs" :runner "target/test/sablono.js"]}}
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
  ;; :cljsbuild {:builds []}
  :prep-tasks [["cljx" "once"]]
  :profiles {:dev {:dependencies [[crate "0.2.5"]
                                  [hickory "0.5.4"]
                                  [reagent "0.4.3"]]
                   :plugins [[com.keminglabs/cljx "0.5.0"]
                             [com.cemerick/austin "0.1.6"]
                             [com.cemerick/clojurescript.test "0.3.3"]
                             [lein-cljsbuild "1.0.4"]]
                   ;; :hooks [cljx.hooks leiningen.cljsbuild]
                   :repl-options {:nrepl-middleware [cljx.repl-middleware/wrap-cljx]}
                   :resource-paths ["test-resources"]
                   :source-paths ["target/classes"]
                   :test-paths ["test" "target/test-classes"]}})
