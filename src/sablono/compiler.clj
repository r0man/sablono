(ns sablono.compiler
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace]]
            [sablono.render :as render]
            [sablono.util :refer [normalize-element react-symbol]])
  (:import cljs.tagged_literals.JSValue))

(defn- unevaluated?
  "True if the expression has not been evaluated."
  [expr]
  (or (symbol? expr)
      (and (seq? expr)
           (not= (first expr) `quote))))

(defn- form-name
  "Get the name of the supplied form."
  [form]
  (if (and (seq? form) (symbol? (first form)))
    (name (first form))))

(declare compile-html)

(defmulti compile-form
  "Pre-compile certain standard forms, where possible."
  {:private true}
  form-name)

(defmethod compile-form "for"
  [[_ bindings body]]
  `(cljs.core/into-array (for ~bindings ~(compile-html body))))

(defmethod compile-form "if"
  [[_ condition & body]]
  `(if ~condition ~@(for [x body] (compile-html x))))

(defmethod compile-form :default
  [expr]
  `(sablono.render/render-html ~expr))

(defn- not-hint?
  "True if x is not hinted to be the supplied type."
  [x type]
  (if-let [hint (-> x meta :tag)]
    (not (isa? (eval hint) type))))

(defn- hint?
  "True if x is hinted to be the supplied type."
  [x type]
  (if-let [hint (-> x meta :tag)]
    (isa? (eval hint) type)))

(defn- literal?
  "True if x is a literal value that can be rendered as-is."
  [x]
  (and (not (unevaluated? x))
       (or (not (or (vector? x) (map? x)))
           (every? literal? x))))

(defn- not-implicit-map?
  "True if we can infer that x is not a map."
  [x]
  (or (= (form-name x) "for")
      (not (unevaluated? x))
      (not-hint? x java.util.Map)))

(defn- element-compile-strategy
  "Returns the compilation strategy to use for a given element."
  [[tag attrs & content :as element]]
  (cond
   (every? literal? element)
   ::all-literal                    ; e.g. [:span "foo"]
   (and (literal? tag) (map? attrs))
   ::literal-tag-and-attributes     ; e.g. [:span {} x]
   (and (literal? tag) (not-implicit-map? attrs))
   ::literal-tag-and-no-attributes  ; e.g. [:span ^String x]
   (literal? tag)
   ::literal-tag                    ; e.g. [:span x]
   :else
   ::default))                      ; e.g. [x]

(declare compile-seq)

(defmulti compile-element
  "Returns an unevaluated form that will render the supplied vector as a HTML
  element."
  {:private true}
  element-compile-strategy)

(defmethod compile-element ::all-literal
  [element]
  (render/render-element (eval element)))

(defmethod compile-element ::literal-tag-and-attributes
  [[tag attrs & content]]
  (let [[tag attrs _] (normalize-element [tag attrs])]
    (if content
      `(~(react-symbol tag) ~(render/js-value attrs) ~@(compile-seq content))
      `(~(react-symbol tag) ~(render/js-value attrs)))))

(defmethod compile-element ::literal-tag-and-no-attributes
  [[tag & content]]
  (compile-element (apply vector tag {} content)))

(defmethod compile-element ::literal-tag
  [[tag attrs & content]]
  (let [[tag tag-attrs _] (normalize-element [tag])
        attrs-sym (gensym "attrs")]
    `(let [~attrs-sym ~attrs]
       (if (map? ~attrs-sym)
         ~(if content
            `(~(react-symbol tag) (sablono.render/render-attrs (sablono.util/merge-with-class ~tag-attrs ~attrs-sym)) ~@(compile-seq content))
            `(~(react-symbol tag) (sablono.render/render-attrs (sablono.util/merge-with-class ~tag-attrs ~attrs-sym)) nil))
         ~(if attrs
            `(~(react-symbol tag) ~(render/js-value tag-attrs) ~@(compile-seq (cons attrs-sym content)))
            `(~(react-symbol tag) ~(render/js-value tag-attrs) nil))))))

(defmethod compile-element :default
  [element]
  `(sablono.render/render-element
    [~(first element)
     ~@(for [x (rest element)]
         (if (vector? x)
           (compile-element x)
           x))]))

(defn- compile-seq
  "Compile a sequence of data-structures into HTML."
  [content]
  (doall (for [expr content]
           (cond
            (vector? expr) (compile-element expr)
            (literal? expr) expr
            (hint? expr String) expr
            (hint? expr Number) expr
            (seq? expr) (compile-form expr)
            :else `(sablono.render/render-html ~expr)))))

(defn compile-html
  "Pre-compile data structures into HTML where possible."
  [& content]
  (let [forms (compile-seq content)]
    (if (> (count forms) 1)
      `(~'into-array ~(vec (compile-seq content)))
      (first forms))))

;; TODO: Remove when landed in ClojureScript.
(defmethod print-method JSValue
  [^JSValue v, ^java.io.Writer w]
  (.write w "#js ")
  (.write w (pr-str (.val v))))
