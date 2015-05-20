(ns sablono.test
  (:require [sablono.core]))

(defmacro html-str [element]
  `(sablono.core/render-static
    (sablono.core/html ~element)))

(defmacro html-vec [element]
  `(some->> (sablono.test/html-str ~element)
            (hickory.core/parse-fragment)
            (map hickory.core/as-hiccup)
            (first)))

(defmacro are-html [& contents]
  `(are [html# expected#]
     (= (sablono.test/html-vec html#) expected# )
     ~@contents))
