(ns user
  (:require [cljs.closure :as closure]))

(time
 (closure/build
  ["src" "test"]
  {:output-to "target/test/sablono.js"
   :output-dir "target/test"
   :optimizations :advanced
   ;; :optimizations :whitespace
   :pretty-print true
   :preamble ["jquery.js"
              "phantomjs-shims.js"]
   :externs ["externs/hickory.js"
             "externs/jquery-1.9.js"]
   :verbose true}))
