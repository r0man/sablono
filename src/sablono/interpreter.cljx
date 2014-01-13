(ns sablono.interpreter
  (:require [clojure.string :refer [blank? join]]
            [sablono.util :refer [html-to-dom-attrs normalize-element]]))

(defprotocol IInterpreter
  (interpret [this] "Interpret a Clojure data structure as a React fn call."))

#+cljs
(defn attributes [attrs]
  (let [attrs (clj->js (html-to-dom-attrs attrs))
        class (join " " (flatten (seq (.-className attrs))))]
    (if-not (blank? class)
      (set! (.-className attrs) class))
    attrs))

#+cljs
(defn element
  "Render an element vector as a HTML element."
  [element]
  (let [[tag attrs content] (normalize-element element)]
    (if-let [dom-fn (aget js/React.DOM (name tag))]
      (dom-fn
       (attributes attrs)
       (cond
        (and (sequential? content)
             (string? (first content))
             (empty? (rest content)))
        (interpret (first content))
        content
        (interpret content)
        :else nil))
      (throw (ex-info "Unsupported HTML tag" {:tag tag :attrs attrs :content content})))))

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
