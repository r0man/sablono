(defproject sablono "0.1.2"
  :description "Lisp style templating for Facebook's React."
  :url "http://github.com/r0man/sablono"
  :author "Roman Scherer"
  :min-lein-version "2.0.0"
  :lein-release {:deploy-via :clojars}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2127"]]
  :cljsbuild {:builds []}
  :profiles {:dev {:dependencies [[crate "0.2.3" :scope "dev"]
                                  [prismatic/dommy "0.1.1"]]
                   :plugins [[com.cemerick/austin "0.1.3"]
                             [com.cemerick/clojurescript.test "0.2.1"]
                             [com.keminglabs/cljx "0.3.2"]
                             [lein-cljsbuild "1.0.1"]]
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
                   :cljsbuild {:builds [{:id "test"
                                         :source-paths ["test" "target/classes" "target/test-classes"]
                                         :compiler {:output-to "target/test/sablono.js"
                                                    :output-dir "target/test"
                                                    :optimizations :whitespace
                                                    :pretty-print true
                                                    :preamble ["jquery.js"
                                                               "phantomjs-shims.js"
                                                               "sablono/react-with-addons-0.8.0.min.js"]
                                                    :externs ["sablono/externs/react.js"]
                                                    :closure-warnings {:non-standard-jsdoc :off}}}]
                               :test-commands {"phantom" ["phantomjs" :runner "target/test/sablono.js"]}}
                   :repl-options {:nrepl-middleware [cljx.repl-middleware/wrap-cljx]}
                   :source-paths ["target/classes"]
                   :test-paths ["test" "target/test-classes"]}})
