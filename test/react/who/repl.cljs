(ns react.who.repl
  (:require [clojure.browser.repl :as repl]))

(defn ^:extern start []
  (repl/connect "http://localhost:9000/repl"))
