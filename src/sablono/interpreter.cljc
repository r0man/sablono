(ns sablono.interpreter
  (:require [clojure.string :refer [blank? join]]
            [sablono.normalize :as normalize]
            [sablono.util :as util]
            #?(:cljs [goog.object :as object])
            #?(:cljs cljsjs.react)))

(defprotocol IInterpreter
  (interpret [this] "Interpret a Clojure data structure as a React fn call."))

#?(:cljs (defn initial-state [component property]
           (let [props #js {}]
             (object/extend props (.-props component) #js {:children (.-children (.-props component))})
             (when (nil? (object/get props property))
               (object/set props property js/undefined))
             props)))

#?(:cljs (defn update-state [component next-props property value]
           (let [next-state #js {}]
             (object/extend next-state next-props #js {:onChange (.-onChange component)})
             (when (.hasOwnProperty (.-props component) property)
               (object/set next-state property (if (nil? value) js/undefined value)))
             (.setState component next-state))))

;; A hack to force input elements to always update itself immediately,
;; without waiting for requestAnimationFrame.

#?(:cljs
   (defn wrap-form-element [element property]
     (js/React.createClass
      #js
      {:displayName (str "wrapped-" element)
       :getInitialState
       (fn []
         (this-as this
           (initial-state this property)))
       :onChange
       (fn [event]
         (this-as this
           (when-let [handler (.-onChange (.-props this))]
             (handler event)
             (update-state this (.-props this) property (object/getValueByKeys event "target" property)))))
       :componentWillReceiveProps
       (fn [new-props]
         (this-as this
           (let [state-value (object/getValueByKeys this "state" property)
                 element-value (object/get (js/ReactDOM.findDOMNode this) property)]
             ;; on IE, onChange event might come after actual value of an element
             ;; have changed. We detect this and render element as-is, hoping that
             ;; next onChange will eventually come and bring our modifications anyways.
             ;; Ignoring this causes skipped letters in controlled components
             ;; https://github.com/reagent-project/reagent/issues/253
             ;; https://github.com/tonsky/rum/issues/86
             (if (not= state-value element-value)
               (update-state this new-props property element-value)
               (update-state this new-props property (object/get new-props property))))))
       :render
       (fn []
         (this-as this
           (js/React.createElement element (.-state this))))})))

#?(:cljs (def wrapped-input (wrap-form-element "input" "value")))
#?(:cljs (def wrapped-checked (wrap-form-element "input" "checked")))
#?(:cljs (def wrapped-select (wrap-form-element "select" "value")))
#?(:cljs (def wrapped-textarea (wrap-form-element "textarea" "value")))

#?(:cljs
   (defn defined? [x]
     (and (not (nil? x))
          (not (undefined? x)))))

#?(:cljs
   (defn element-class [type props]
     (case (name type)
       "input"
       (case (and (object? props) (.-type props))
         "radio" wrapped-checked
         "checkbox" wrapped-checked
         wrapped-input)
       "select" wrapped-select
       "textarea" wrapped-textarea
       (name type))))

#?(:cljs
   (defn create-element [type props & children]
     (let [class (element-class type props)
           children (remove nil? children)]
       (if (empty? children)
         (js/React.createElement class props)
         (apply js/React.createElement class props children)))))

#?(:cljs
   (defn attributes [attrs]
     (let [js-attrs (clj->js (util/html-to-dom-attrs attrs))
           class (.-className js-attrs)
           class (if (array? class) (join " " class) class)]
       (if (blank? class)
         (js-delete js-attrs "className")
         (set! (.-className js-attrs) class))
       ;; TODO: clj->js converts js/undefined to nil
       (when (undefined? (:value attrs))
         (set! (.-value js-attrs) js/undefined))
       js-attrs)))

(defn- interpret-seq
  "Interpret the seq `x` as HTML elements."
  [x]
  (map interpret x))

#?(:cljs
   (defn element
     "Render an element vector as a HTML element."
     [element]
     (let [[type attrs content] (normalize/element element)]
       (apply create-element type
              (attributes attrs)
              (interpret-seq content)))))

#?(:cljs
   (defn- interpret-vec
     "Interpret the vector `x` as an HTML element or a the children of
  an element."
     [x]
     (if (util/element? x)
       (element x)
       (interpret-seq x))))

#?(:cljs
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
     Subvec
     (interpret [this]
       (interpret-vec this))
     PersistentVector
     (interpret [this]
       (interpret-vec this))
     default
     (interpret [this]
       this)
     nil
     (interpret [this]
       nil)))
