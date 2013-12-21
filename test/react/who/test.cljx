(ns react.who.test
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace]]
            [react.who.core :as core]))

(defmacro are-html-rendered [& body]
  `(cemerick.cljs.test/are [form# expected#]
     (cemerick.cljs.test/is (= expected# (react.who.core/render-dom (react.who.core/html form#))))
     ~@body))
