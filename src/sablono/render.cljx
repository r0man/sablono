(ns sablono.render
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [blank? join replace split]]
            [clojure.walk :refer [postwalk]]
            [sablono.util :refer [normalize-element react-symbol]]))

(defprotocol IRender
  (interpret [this] "Render a Clojure data structure via Facebook's React."))

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
      (dom-fn (render-attrs attrs) (interpret content))
      (dom-fn (render-attrs attrs)))))

(defn- render-seq [s]
  (into-array (map interpret s)))

#+cljs
(extend-protocol IRender
  Cons
  (interpret [this]
    (render-seq this))
  ChunkedSeq
  (interpret [this]
    (render-seq this))
  LazySeq
  (interpret [this]
    (render-seq this))
  List
  (interpret [this]
    (render-seq this))
  IndexedSeq
  (interpret [this]
    (render-seq this))
  PersistentVector
  (interpret [this]
    (render-element this))
  default
  (interpret [this]
    this)
  nil
  (interpret [this]
    nil))
