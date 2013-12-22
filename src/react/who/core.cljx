(ns react.who.core
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace]]
            #+clj [react.who.compiler :as compiler]))

#+clj
(defmacro html
  "Render Clojure data structures via Facebook's React."
  [options & content]
  (apply react.who.compiler/compile-html options content))

#+clj
(defmacro html-expand
  "Returns the expanded HTML generation forms."
  [& forms]
  `(macroexpand `(html ~~@forms)))

(defn include-js
  "Include a list of external javascript files."
  [& scripts]
  (for [script scripts]
    [:script {:type "text/javascript", :src (str script)}]))

(defn include-css
  "Include a list of external stylesheet files."
  [& styles]
  (for [style styles]
    [:link {:type "text/css", :href (str style), :rel "stylesheet"}]))
