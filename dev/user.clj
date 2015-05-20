(ns user
  (:require [cljs.build.api :as build]))


(defn rebuild []
  (build/build
   (build/inputs "src" "test")
   {:output-to "target/test/sablono.js"
    :output-dir "target/test/out"
    ;; :optimizations :advanced
    ;; :optimizations :whitespace
    ;; :pretty-print true
    :preamble ["jquery.js"
               "phantomjs-shims.js"]
    :externs ["externs/hickory.js"
              "externs/jquery-1.9.js"]
    :verbose true}))

(rebuild)
