(ns sablono.render
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [blank? join replace split]]
            [clojure.walk :refer [postwalk]]
            [sablono.util :refer [normalize-element react-symbol]])
  #+clj (:import cljs.tagged_literals.JSValue))

(defprotocol HtmlRenderer
  (render-html [this] "Render a Clojure data structure via Facebook's React."))

#+cljs
(defn render-attrs [attrs]
  (let [attrs (clj->js attrs)
        class (join " " (flatten (seq (.-className attrs))))]
    (if-not (blank? class)
      (set! (.-className attrs) class))
    attrs))

#+cljs
(defn render-element
  "Render an element vector as a HTML element."
  [element]
  (let [[tag attrs content] (normalize-element element)
        dom-fn (aget js/React.DOM (name tag))]
    (if content
      (dom-fn (render-attrs attrs) (render-html content))
      (dom-fn (render-attrs attrs)))))

(defn- render-seq [s]
  (into-array (map render-html s)))

#+cljs
(extend-protocol HtmlRenderer
  Cons
  (render-html [this]
    (render-seq this))
  ChunkedSeq
  (render-html [this]
    (render-seq this))
  LazySeq
  (render-html [this]
    (render-seq this))
  List
  (render-html [this]
    (render-seq this))
  IndexedSeq
  (render-html [this]
    (render-seq this))
  PersistentVector
  (render-html [this]
    (render-element this))
  default
  (render-html [this]
    this)
  nil
  (render-html [this]
    nil))
