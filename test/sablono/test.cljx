(ns sablono.test
  (:require [sablono.core :as core]))

(defmacro html-str [element]
  `(sablono.core/render-static
    (sablono.core/html ~element)))
