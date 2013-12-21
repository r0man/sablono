(ns react.who.core
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace]]
            #+clj [react.who.compiler :as compiler]))

#+clj
(defmacro html
  "Render Clojure data structures via Facebook's React."
  [options & content]
  (apply compiler/compile-html options content))

#+clj
(defmacro html-expand
  "Returns the expanded HTML generation forms."
  [& forms]
  `(macroexpand `(html ~~@forms)))
