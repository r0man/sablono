(ns sablono.element
  (:refer-clojure :exclude [symbol type])
  (:require [cljsjs.react]
            ;; [cljsjs.react.dom.server]
            [cljsjs.react.dom]
            [goog.object :as gobj]
            [sablono.server :as server]))

(defn symbol
  "Return a JavaScript symbol."
  [name]
  ((gobj/get js/Symbol "for") name))

(defn props
  "Return the properties of a React element."
  [attributes children]
  (let [props (clj->js attributes)]
    (gobj/set props "children" (clj->js children))
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
  (type (create "div" {:className "x"} ["a"]))

  (prn (js/React.createElement "div" #js {:className "x"} "a"))
  (prn (create "div" {} []))

  (= (server/render-static (js/React.createElement "div" #js {:className "x"} #js ["a"]))
     (server/render-static (create "div" {:className "x"} ["a"])))


  (js/ReactDOM.(js/React.createElement "div")))
