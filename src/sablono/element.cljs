(ns sablono.element
  (:refer-clojure :exclude [symbol type])
  (:require [cljsjs.react]
            [cljsjs.react.dom]
            [cljsjs.react.dom.server]
            [goog.object :as gobj]
            [sablono.server :as server]))

(defn symbol
  "Return a JavaScript symbol."
  [name]
  ((gobj/get js/Symbol "for") name))

(defn props
  "Return the properties of a React element."
  [attributes children]
  (let [props (clj->js (or attributes {}))]
    (gobj/set props "children" (clj->js (vec (or children []))))
    props))

(defn create
  "Create a React element."
  [type attributes children]
  #js {:$$typeof (symbol "react.element")
       :props (props attributes children)
       :type type})

(defn type
  "Return the type of `element`."
  [element]
  (gobj/get element "type"))

(comment
  (js/ReactDOMServer.renderToStaticMarkup (create "div" {:className "x"} ["a"]))
  (create "div" {:className "x"} ["a"])
  (create "div" {:className "x"} ["a"]))
