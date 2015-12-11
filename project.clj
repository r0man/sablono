(defproject sablono "0.5.2-SNAPSHOT"
  :description "Lisp style templating for Facebook's React."
  :url "http://github.com/r0man/sablono"
  :author "r0man"
  :min-lein-version "2.0.0"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[cljsjs/react "0.14.3-0" :scope "provided"]
                 [cljsjs/react-dom "0.14.3-1" :scope "provided"]
                 [cljsjs/react-dom-server "0.14.3-0" :scope "provided"]
                 [org.clojure/clojure "1.7.0" :scope "provided"]
                 [org.clojure/clojurescript "1.7.189" :scope "provided"]]
  :aliases {"ci" ["do"
                  ["clean"]
                  ["test"]
                  ["doo" "phantom" "none" "once"]
                  ["doo" "phantom" "advanced" "once"]]
            "deploy" ["do" "clean," "deploy" "clojars"]}
  :cljsbuild {:builds
              [{:id "none"
                :compiler
                {:asset-path "target/none/out"
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
                {:asset-path "target/advanced/out"
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
                                  [hickory "0.5.4"]
                                  [reagent "0.5.1"]]
                   :plugins [[lein-cljsbuild "1.1.1"]
                             [lein-doo "0.1.6"]]
                   :resource-paths ["test-resources"]}
             :repl {:dependencies [[com.cemerick/piggieback "0.2.1"]]
                    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}})
