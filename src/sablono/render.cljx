(ns sablono.render
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [blank? join replace split]]
            [clojure.walk :refer [postwalk]]
            [sablono.util :refer [normalize-element react-symbol]])
  #+clj (:import cljs.tagged_literals.JSValue))

(defprotocol HtmlRenderer
  (render-html [this] "Render a Clojure data structure via Facebook's React."))

(defprotocol IJSValue
  (to-js [x]))

#+clj
(defn- to-js-map [x]
  (JSValue.
   (zipmap (map to-js (keys x))
           (map to-js (vals x)))))

#+clj
(extend-protocol IJSValue
  clojure.lang.PersistentArrayMap
  (to-js [x]
    (to-js-map x))
  clojure.lang.PersistentHashMap
  (to-js [x]
    (to-js-map x))
  clojure.lang.PersistentVector
  (to-js [x]
    (JSValue. (vec (map to-js x))))
  Object
  (to-js [x]
    x))

#+clj
(defn js-value [attrs]
  (let [classes (:className attrs)]
    (if (empty? classes)
      (to-js attrs)
      (->> (cond
            (or (keyword? classes)
                (string? classes))
            classes
            (and (sequential? classes)
                 (= 1 (count classes)))
            (first classes)
            (and (sequential? classes)
                 (every? string? classes))
            (join " " classes)
            :else `(sablono.util/join-classes ~classes))
           (assoc attrs :className)
           (to-js)))))

#+cljs
(defn render-attrs [attrs]
  (let [attrs (clj->js attrs)
        class (join " " (flatten (seq (.-className attrs))))]
    (if-not (blank? class)
      (set! (.-className attrs) class))
    attrs))

#+clj
(defn render-element
  "Render an element vector as a HTML element."
  [element]
  (let [[tag attrs content] (normalize-element element)]
    (if content
      `(~(react-symbol tag) ~(js-value attrs) ~@(render-html content))
      `(~(react-symbol tag) ~(js-value attrs)))))

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

#+clj
(extend-protocol HtmlRenderer
  clojure.lang.IPersistentVector
  (render-html [this]
    (render-element this))
  clojure.lang.ISeq
  (render-html [this]
    (map render-html this))
  Object
  (render-html [this]
    this)
  nil
  (render-html [this]
    nil))

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
