(ns sablono.compiler
  (:require [sablono.util :refer :all])
  (:import cljs.tagged_literals.JSValue))

(defprotocol ICompile
  (compile-react [this] "Compile a Clojure data structure into a React fn call."))

(defprotocol IJSValue
  (to-js [x]))

(defn compile-map-attr [name value]
  {name
   (if (map? value)
     (to-js value)
     `(~'clj->js ~value))})

(defn compile-string-attr [name value]
  {name value})

(defmulti compile-attr (fn [name value] name))

(defmethod compile-attr :class [name value]
  {:class
   (cond
    (or (keyword? value)
        (string? value))
    value
    (and (sequential? value)
         (= 1 (count value)))
    (first value)
    (and (sequential? value)
         (every? string? value))
    (join-classes value)
    :else `(sablono.util/join-classes ~value))})

(defmethod compile-attr :style [name value]
  (compile-map-attr name value))

(defmethod compile-attr :default [name value]
  (compile-string-attr name value))

(defn compile-attrs
  "Compile a HTML attribute map."
  [attrs]
  (->> (seq attrs)
       (map #(apply compile-attr %1))
       (apply merge)
       (html-to-dom-attrs)
       (to-js)))

(defn compile-merge-attrs [attrs-1 attrs-2]
  (let [empty-attrs? #(or (nil? %1) (and (map? %1) (empty? %1)))]
    (cond
     (and (empty-attrs? attrs-1)
          (empty-attrs? attrs-2))
     nil
     (empty-attrs? attrs-1)
     `(sablono.interpreter/attributes ~attrs-2)
     (empty-attrs? attrs-2)
     `(sablono.interpreter/attributes ~attrs-1)
     (and (map? attrs-1)
          (map? attrs-2))
     (merge-with-class attrs-1 attrs-2)
     :else `(sablono.interpreter/attributes
             (sablono.util/merge-with-class ~attrs-1 ~attrs-2)))))

(defn compile-react-element
  "Render an element vector as a HTML element."
  [element]
  (let [[tag attrs content] (normalize-element element)]
    (if content
      `(~(react-fn tag) ~(compile-attrs attrs) ~@(compile-react content))
      `(~(react-fn tag) ~(compile-attrs attrs)))))

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
  `(~'into-array (for ~bindings ~(compile-html body))))

(defmethod compile-form "if"
  [[_ condition & body]]
  `(if ~condition ~@(for [x body] (compile-html x))))

(defmethod compile-form :default
  [expr]
  `(sablono.interpreter/interpret ~expr))

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
  (compile-react-element (eval element)))

(defmethod compile-element ::literal-tag-and-attributes
  [[tag attrs & content]]
  (let [[tag attrs _] (normalize-element [tag attrs])]
    (if content
      `(~(react-fn tag) ~(compile-attrs attrs) ~@(compile-seq content))
      `(~(react-fn tag) ~(compile-attrs attrs)))))

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
            `(~(react-fn tag) ~(compile-merge-attrs tag-attrs attrs-sym) ~@(compile-seq content))
            `(~(react-fn tag) ~(compile-merge-attrs tag-attrs attrs-sym) nil))
         ~(if attrs
            `(~(react-fn tag) ~(compile-attrs tag-attrs) ~@(compile-seq (cons attrs-sym content)))
            `(~(react-fn tag) ~(compile-attrs tag-attrs) nil))))))

(defmethod compile-element :default
  [element]
  `(sablono.interpreter/interpret
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
            :else `(sablono.interpreter/interpret ~expr)))))

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

(extend-protocol ICompile
  clojure.lang.IPersistentVector
  (compile-react [this]
    (compile-react-element this))
  clojure.lang.ISeq
  (compile-react [this]
    (map compile-react this))
  Object
  (compile-react [this]
    this)
  nil
  (compile-react [this]
    nil))

(defn- to-js-map [x]
  (JSValue.
   (zipmap (map to-js (keys x))
           (map to-js (vals x)))))

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
    x)
  nil
  (to-js [_]
    nil))
