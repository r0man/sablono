(defproject react-who "0.1.0-SNAPSHOT"
  :description "Lisp style templating for Facebook's React."
  :url "http://github.com/r0man/react-who"
  :author "Roman Scherer"
  :min-lein-version "2.0.0"
  :lein-release {:deploy-via :clojars}
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/clojurescript "0.0-2127"]]
  :profiles {:dev {:dependencies [[com.keminglabs/cljx "0.3.1"]]
                   :plugins [[com.cemerick/austin "0.1.3"]]
                   :repl-options {:nrepl-middleware [cljx.repl-middleware/wrap-cljx]}}}
  :plugins [[com.cemerick/clojurescript.test "0.2.1"]
            [com.keminglabs/cljx "0.3.1"]
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
  :cljsbuild {:builds [{:source-paths ["test" "target/classes" "target/test-classes"]
                        :compiler {:output-to "target/testable.js"
                                   :optimizations :simple
                                   :pretty-print true
                                   :foreign-libs [{:file "http://fb.me/react-0.8.0.js" :provides ["React"]}]
                                   :closure-warnings {:non-standard-jsdoc :off}}}]
              :test-commands {"phantom" ["phantomjs" :runner "test-resources/phantomjs-shims.js" "target/testable.js"]}}
  :test-paths ["test" "target/test-classes"])
