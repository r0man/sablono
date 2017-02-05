(ns sablono.test
  (:require [clojure.test.check]
            [clojure.test.check.generators]
            [sablono.core]))

(defmacro html-str [element]
  `(sablono.server/render-static
    (sablono.core/html ~element)))

(defmacro html-data [element]
  `(some-> (sablono.test/html-str ~element)
           (tubax.core/xml->clj)))
