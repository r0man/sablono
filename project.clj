(defproject sablono "0.3.5"
  :description "Lisp style templating for Facebook's React."
  :url "http://github.com/r0man/sablono"
  :author "r0man"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cljsjs/react "0.13.3-0"]
                 [org.clojure/clojure "1.7.0-beta3"]
                 [org.clojure/clojurescript "0.0-3308" :scope "provided"]]
  :aliases {"cleantest" ["do" "clean," "test," "cljsbuild" "test"]
            "deploy" ["do" "clean," "deploy" "clojars"]}
  :cljsbuild {:builds
              [{:id "none"
                :compiler
                {:asset-path "../../target/none/out"
                 :main sablono.test
                 :output-to "target/none/sablono.js"
                 :output-dir "target/none/out"
                 :optimizations :none
                 :pretty-print true
                 :source-map true
                 :verbose true}
                :notify-command ["bin/phantomjs" "none"]
                :source-paths ["src" "test"]}
               {:id "advanced"
                :compiler
                {:asset-path "../../target/advanced/out"
                 :main sablono.test
                 :output-to "target/advanced/sablono.js"
                 :optimizations :advanced
                 :pretty-print true
                 :externs ["externs/hickory.js"]
                 :verbose true}
                :notify-command ["bin/phantomjs" "advanced"]
                :source-paths ["src" "test"]}]
              :test-commands {"phantom" ["bin/phantomjs"]}}
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev {:dependencies [[crate "0.2.5"]
                                  [cljsjs/jquery "2.1.4-0"]
                                  [com.cemerick/piggieback "0.2.1"]
                                  [org.clojure/tools.nrepl "0.2.10"]
                                  [org.clojure/tools.reader "0.9.2"]
                                  [hickory "0.5.4"]
                                  [reagent "0.5.0"]]
                   :plugins [[com.cemerick/clojurescript.test "0.3.3"]
                             [lein-cljsbuild "1.0.6"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   :resource-paths ["test-resources"]}})
