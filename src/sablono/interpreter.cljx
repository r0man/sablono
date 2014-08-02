(ns sablono.interpreter
  (:require [clojure.string :refer [blank? join]]
            [sablono.util :refer [html-to-dom-attrs normalize-element]]))

(defprotocol IInterpreter
  (interpret [this] "Interpret a Clojure data structure as a React fn call."))

;; Taken from om, to hack around form elements.

#+cljs
(defn wrap-form-element [ctor display-name]
  (js/React.createClass
   #js
   {:getDisplayName
    (fn [] display-name)
    :getInitialState
    (fn []
      (this-as this #js {:value (aget (.-props this) "value")}))
    :onChange
    (fn [e]
      (this-as
       this
       (let [handler (aget (.-props this) "onChange")]
         (when-not (nil? handler)
           (handler e)
           (.setState this #js {:value (.. e -target -value)})))))
    :componentWillReceiveProps
    (fn [new-props]
      (this-as this (.setState this #js {:value (aget new-props "value")})))
    :render
    (fn []
      (this-as
       this
       (.transferPropsTo
        this
        (ctor #js {:value (aget (.-state this) "value")
                   :onChange (aget this "onChange")
                   :children (aget (.-props this) "children")}))))}))

#+cljs (def input (wrap-form-element js/React.DOM.input "Input"))
#+cljs (def option (wrap-form-element js/React.DOM.option "Option"))
#+cljs (def textarea (wrap-form-element js/React.DOM.textarea "Textarea"))

#+cljs
(defn dom-fn [tag]
  (if-let [dom-fn (aget js/React.DOM (name tag))]
    (get {:input sablono.interpreter/input
          :textarea sablono.interpreter/textarea}
         (keyword tag) dom-fn)
    (throw (ex-info (str "Unsupported HTML tag: " (name tag)) {:tag tag}))))

#+cljs
(defn attributes [attrs]
  (let [attrs (clj->js (html-to-dom-attrs attrs))
        class (.-className attrs)
        class (if (array? class) (join " " class) class)]
    (if (blank? class)
      (js-delete attrs "className")
      (set! (.-className attrs) class))
    attrs))

#+cljs
(defn element
  "Render an element vector as a HTML element."
  [element]
  (let [[tag attrs content] (normalize-element element)
        f (dom-fn tag)
        js-attrs (attributes attrs)]
    (cond
     (and (sequential? content)
          (= 1 (count content)))
     (f js-attrs (interpret (first content)))
     content
     (apply f js-attrs (interpret content))
     :else (f js-attrs nil))))

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
