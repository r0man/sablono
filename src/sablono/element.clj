(ns sablono.element
  (:refer-clojure :exclude [symbol type])
  (:import cljs.tagged_literals.JSValue))

(defn- symbol
  "Return a JavaScript symbol."
  [name]
  'sablono.element.react)

(defn- make-children
  "Build the children of a React element."
  [children]
  (JSValue. (vec (or children []))))

(defn- make-props
  "Build the properties of a React element."
  [attributes children]
  (JSValue. (assoc attributes :children (make-children children))))

(defn create
  "Create a React element."
  [type attributes children]
  (JSValue.
   {:$$typeof (symbol "react.element")
    :props (make-props attributes children)
    :type type}))

(defn type
  "Return the type of `element`."
  [element]
  (:type (.val element)))

(defn children
  "Return the children of `element`."
  [element]
  (some-> (.val element) :props .val :children))

(defn attributes
  "Return the attributes of `element`."
  [element]
  (some-> (.val element) :props .val
          (dissoc :children)))

(comment
  (type (create "div" {:className "x"} ["a"])s)
  (create "div" {:className "x"} ["a"]))
