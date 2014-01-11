(ns sablono.interpreter
  (:require [clojure.string :refer [blank? join]]
            [sablono.util :refer [normalize-element]]))

(defprotocol IInterpreter
  (interpret [this] "Interpret a Clojure data structure as a React fn call."))

#+cljs
(defn attributes [attrs]
  (let [class (join " " (flatten (seq (:class attrs))))
        attrs (clj->js (dissoc attrs :class))]
    (if-not (blank? class)
      (set! (.-className attrs) class))
    attrs))

#+cljs
(defn element
  "Render an element vector as a HTML element."
  [element]
  (let [[tag attrs content] (normalize-element element)
        dom-fn (aget js/React.DOM (name tag))]
    (if content
      (dom-fn (attributes attrs) (interpret content))
      (dom-fn (attributes attrs)))))

(defn- interpret-seq [s]
  (into-array (map interpret s)))

#+cljs
(extend-protocol IInterpreter
  Cons
  (interpret [this]
    (interpret-seq this))
  ChunkedSeq
  (interpret [this]
    (interpret-seq this))
  LazySeq
  (interpret [this]
    (interpret-seq this))
  List
  (interpret [this]
    (interpret-seq this))
  IndexedSeq
  (interpret [this]
    (interpret-seq this))
  PersistentVector
  (interpret [this]
    (element this))
  default
  (interpret [this]
    this)
  nil
  (interpret [this]
    nil))
