(defproject sablono "0.2.9-SNAPSHOT"
  :description "Lisp style templating for Facebook's React."
  :url "http://github.com/r0man/sablono"
  :author "Roman Scherer"
  :min-lein-version "2.0.0"
  :lein-release {:deploy-via :clojars}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[com.facebook/react "0.8.0.1"]
                 [org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2173"]]
  :cljsbuild {:builds []}
  :profiles {:dev {:dependencies [[reagent "0.4.2"]
                                  [crate "0.2.5"]
                                  [prismatic/dommy "0.1.2"]]
                   :plugins [[com.keminglabs/cljx "0.3.2"] ;; Must be before Austin: https://github.com/cemerick/austin/issues/37
                             [com.cemerick/austin "0.1.3"]
                             [com.cemerick/clojurescript.test "0.2.2"]
                             [lein-cljsbuild "1.0.2"]]
                   :hooks [cljx.hooks leiningen.cljsbuild]
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
                                                    :externs ["react/externs/react.js"
                                                              "externs/jquery-1.9.js"]}}]
                               :test-commands {"phantom" ["phantomjs" :runner "target/test/sablono.js"]}}
                   :repl-options {:nrepl-middleware [cljx.repl-middleware/wrap-cljx]}
                   :resource-paths ["test-resources"]
                   :source-paths ["target/classes"]
                   :test-paths ["test" "target/test-classes"]}})
