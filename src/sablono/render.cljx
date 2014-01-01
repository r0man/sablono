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
(defn interpret-element
  "Render an element vector as a HTML element."
  [element]
  (let [[tag attrs content] (normalize-element element)
        dom-fn (aget js/React.DOM (name tag))]
    (if content
      (dom-fn (render-attrs attrs) (interpret content))
      (dom-fn (render-attrs attrs)))))

(defn- interpret-seq [s]
  (into-array (map interpret s)))

#+cljs
(extend-protocol IRender
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
    (interpret-element this))
  default
  (interpret [this]
    this)
  nil
  (interpret [this]
    nil))
