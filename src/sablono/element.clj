(ns sablono.element
  (:refer-clojure :exclude [symbol type])
  (:import cljs.tagged_literals.JSValue))

(defn symbol
  "Return a JavaScript symbol."
  [name]
  nil)

(defn props
  "Return the properties of a React element."
  [attributes children]
  (JSValue. (assoc attributes :children (JSValue. children))))

(defn create
  "Create a React element."
  [type attributes children]
  (JSValue.
   {:$$typeof (symbol "react.element")
    :props (props attributes children)
    :type type}))

(defn type
  "Return the type of `element`."
  [element]
  (:type (.val element)))

(comment
  (type (create "div" {:className "x"} ["a"]))
  (create "div" {:className "x"} ["a"]))
