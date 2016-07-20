(ns sablono.interpreter
  (:require [clojure.string :refer [blank? join]]
            [sablono.normalize :as normalize]
            [sablono.util :as util]
            #?(:cljs [goog.object :as gobject])
            #?(:cljs cljsjs.react)))

(defprotocol IInterpreter
  (interpret [this] "Interpret a Clojure data structure as a React fn call."))

;; A hack to force input elements to always update itself immediately, without
;; waiting for requestAnimationFrame

#?(:cljs
   (defn wrap-form-element [element]
     (js/React.createClass
       #js
       {:displayName (str "wrapped-" element)
        :getInitialState
        (fn []
          (this-as this
            #js {"state_value" (aget (.-props this) "value")}))
        :onChange
        (fn [e]
          (this-as this
            (let [handler (aget (.-props this) "onChange")]
              (when-not (nil? handler)
                (handler e)
                (.setState this #js {"state_value" (.. e -target -value)})))))
        :componentWillReceiveProps
        (fn [new-props]
          (this-as this
            (let [state-value   (aget (.-state this) "state_value")
                  element       (js/ReactDOM.findDOMNode this)
                  element-value (.-value element)]
              ;; on IE, onChange event might come after actual value of an element
              ;; have changed. We detect this and render element as-is, hoping that
              ;; next onChange will eventually come and bring our modifications anyways.
              ;; Ignoring this causes skipped letters in controlled components
              ;; https://github.com/reagent-project/reagent/issues/253
              ;; https://github.com/tonsky/rum/issues/86
              (if (not= state-value element-value)
                (.setState this #js {"state_value" element-value})
                (.setState this #js {"state_value" (aget new-props "value")})))))
        :render
        (fn []
          (this-as this
            ;; NOTE: if switch to macro we remove a closure allocation
            (let [element-props #js {}]
              (gobject/extend
                element-props
                (.-props this)
                #js {:value    (or (aget (.-state this) "state_value") js/undefined)
                     :onChange (aget this "onChange")
                     :children (aget (.-props this) "children")})
              (js/React.createElement element element-props))))})))

#?(:cljs (def wrapped-input (wrap-form-element "input")))
#?(:cljs (def wrapped-select (wrap-form-element "select")))
#?(:cljs (def wrapped-textarea (wrap-form-element "textarea")))

#?(:cljs
   (defn create-element [type props & children]
     (let [class (case (keyword type)
                   :input
                   (if (and props (or (exists? (.-checked props))
                                      (exists? (.-value props))))
                     wrapped-input "input")
                   :select
                   (if (and props (exists? (.-value props)))
                     wrapped-select "select")
                   :textarea
                   (if (and props (exists? (.-value props)))
                     wrapped-textarea "textarea")
                   (name type))
           children (remove nil? children)]
       (if (empty? children)
         (js/React.createElement class props)
         (apply js/React.createElement class props children)))))

#?(:cljs
   (defn attributes [attrs]
     (let [attrs (clj->js (util/html-to-dom-attrs attrs))
           class (.-className attrs)
           class (if (array? class) (join " " class) class)]
       (if (blank? class)
         (js-delete attrs "className")
         (set! (.-className attrs) class))
       attrs)))

(defn- interpret-seq
  "Interpret the seq `x` as HTML elements."
  [x]
  (into [] (map interpret) x))

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
