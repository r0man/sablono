(ns sablono.compiler
  (:require [cljs.analyzer :as ana]
            [cljs.compiler :as cljs]
            [clojure.set :as set]
            [sablono.normalize :as normalize]
            [sablono.util :refer :all])
  ;; TODO: Fix emit-constant exception for JSValue.
  ;; Require of cljs.tagged_literals causes trouble, but a require of
  ;; cljs.compiler seems to work. Also, switching JSValue to a record
  ;; in ClojureScript seems to fix the problem.
  (:import cljs.tagged_literals.JSValue))

(defprotocol ICompile
  (compile-react [this env]
    "Compile a Clojure data structure into code that returns a React element."))

(defprotocol IJSValue
  (to-js [x]))

(def ^:private primitive-types
  "The set of primitive types that can be handled by React."
  #{'clj-nil 'js/React.Element 'number 'string 'sablono.html.Element})

(defn- primitive-type?
  "Return true if `tag` is a primitive type that can be handled by
  React, otherwise false. "
  [tags]
  (and (not (empty? tags)) (set/subset? tags primitive-types)))

(defn infer-tag
  "Infer the tag of `form` using `env`."
  [env form]
  (when env
    (when-let [tags (ana/infer-tag env (ana/no-warn (ana/analyze env form)))]
      (if (set? tags) tags (set [tags])))))

(defn fragment?
  "Returns true if `tag` is the fragment tag \"*\", otherwise false."
  [tag]
  (= (name tag) "*"))

(defmulti compile-attr (fn [name value] name))

(defmethod compile-attr :class [_ value]
  (cond
    (or (nil? value)
        (keyword? value)
        (string? value))
    value
    (and (or (sequential? value)
             (set? value))
         (every? string? value))
    (join-classes value)
    :else `(sablono.util/join-classes ~value)))

(defmethod compile-attr :style [_ value]
  (let [value (camel-case-keys value)]
    (if (map? value)
      (to-js value)
      `(sablono.interpreter/attributes ~value))))

(defmethod compile-attr :default [_ value]
  (to-js value))

(defn compile-attrs
  "Compile a HTML attribute map."
  [attrs]
  (->> (seq attrs)
       (reduce (fn [attrs [name value]]
                 (assoc attrs name (compile-attr name value) ))
               nil)
       (html-to-dom-attrs)
       (to-js)))

(defn- compile-constructor
  "Return the symbol of a fn that build a React element. "
  [type]
  (if (contains? #{:input :select :textarea} (keyword type))
    'sablono.interpreter/create-element
    'sablono.core/create-element))

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
      (normalize/merge-with-class attrs-1 attrs-2)
      :else `(sablono.interpreter/attributes
              (sablono.normalize/merge-with-class ~attrs-1 ~attrs-2)))))

(defn- compile-tag
  "Replace fragment syntax (`:*`) by 'React.Fragment, otherwise the
  name of the tag"
  [tag]
  (if (fragment? tag)
    'sablono.core/fragment
    (name tag)))

(defn compile-react-element
  "Render an element vector as a HTML element."
  [element env]
  (let [[tag attrs content] (normalize/element element)]
    `(~(compile-constructor tag)
      ~(compile-tag tag)
      ~(compile-attrs attrs)
      ~@(if content (compile-react content env)))))

(defn- unevaluated?
  "True if the expression has not been evaluated."
  [expr]
  (or (symbol? expr)
      (and (seq? expr)
           (not= (first expr) `quote))))

(defmacro interpret-maybe
  "Macro that wraps `expr` with a call to
  `sablono.interpreter/interpret` if the inferred return type is not a
  primitive React type."
  [expr]
  (if (primitive-type? (infer-tag &env expr))
    expr `(sablono.interpreter/interpret ~expr)))

(defn- form-name
  "Get the name of the supplied form."
  [form]
  (if (and (seq? form) (symbol? (first form)))
    (name (first form))))

(declare compile-html)

(defmulti compile-form
  "Pre-compile certain standard forms, where possible."
  {:private true}
  (fn [form env] (form-name form)))

(defmethod compile-form "case"
  [[_ v & cases] env]
  `(case ~v
     ~@(doall (mapcat
               (fn [[test hiccup]]
                 (if hiccup
                   [test (compile-html hiccup env)]
                   [(compile-html test env)]))
               (partition-all 2 cases)))))

(defmethod compile-form "cond"
  [[_ & clauses] env]
  `(cond ~@(mapcat
            (fn [[check expr]]
              [check (compile-html expr env)])
            (partition 2 clauses))))

(defmethod compile-form "condp"
  [[_ f v & cases] env]
  `(condp ~f ~v
     ~@(doall (mapcat
               (fn [[test hiccup]]
                 (if hiccup
                   [test (compile-html hiccup env)]
                   [(compile-html test env)]))
               (partition-all 2 cases)))))

(defmethod compile-form "do"
  [[_ & forms] env]
  `(do ~@(butlast forms) ~(compile-html (last forms) env)))

(defmethod compile-form "let"
  [[_ bindings & body] env]
  `(let ~bindings ~@(butlast body) ~(compile-html (last body) env)))

(defmethod compile-form "let*"
  [[_ bindings & body] env]
  `(let* ~bindings ~@(butlast body) ~(compile-html (last body) env)))

(defmethod compile-form "letfn*"
  [[_ bindings & body] env]
  `(letfn* ~bindings ~@(butlast body) ~(compile-html (last body) env)))

(defmethod compile-form "for"
  [[_ bindings body] env]
  `(~'into-array (for ~bindings ~(compile-html body env))))

(defmethod compile-form "if"
  [[_ condition & body] env]
  `(if ~condition ~@(for [x body] (compile-html x env))))

(defmethod compile-form "if-not"
  [[_ bindings & body] env]
  `(if-not ~bindings ~@(doall (for [x body] (compile-html x env)))))

(defmethod compile-form "if-some"
  [[_ bindings & body] env]
  `(if-some ~bindings ~@(doall (for [x body] (compile-html x env)))))

(defmethod compile-form "when"
  [[_ bindings & body] env]
  `(when ~bindings ~@(doall (for [x body] (compile-html x env)))))

(defmethod compile-form "when-not"
  [[_ bindings & body] env]
  `(when-not ~bindings ~@(doall (for [x body] (compile-html x env)))))

(defmethod compile-form "when-some"
  [[_ bindings & body] env]
  `(when-some ~bindings ~@(butlast body) ~(compile-html (last body) env)))

(defmethod compile-form :default
  [expr env]
  (if (:inline (meta expr))
    expr `(interpret-maybe ~expr)))

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

(defn- attrs-hint?
  "True if x has :attrs metadata. Treat x as a implicit map"
  [x]
  (-> x meta :attrs))

(defn- inline-hint?
  "True if x has :inline metadata. Treat x as a implicit map"
  [x]
  (-> x meta :inline))

(defn- element-compile-strategy
  "Returns the compilation strategy to use for a given element."
  [[tag attrs & content :as element] env]
  (cond
    ;; e.g. [:span "foo"]
    (every? literal? element)
    ::all-literal

    ;; e.g. [:span {} x]
    (and (literal? tag) (map? attrs))
    ::literal-tag-and-attributes

    ;; e.g. [:span ^String x]
    (and (literal? tag) (not-implicit-map? attrs))
    ::literal-tag-and-no-attributes

    ;; e.g. [:span (attrs)], return type of `attrs` is a map
    (and (literal? tag)
         (= '#{cljs.core/IMap} (infer-tag env attrs)))
    ::literal-tag-and-hinted-attributes

    ;; e.g. [:span ^:attrs y]
    (and (literal? tag) (attrs-hint? attrs))
    ::literal-tag-and-hinted-attributes

    ;; e.g. [:span ^:inline (y)]
    (and (literal? tag) (inline-hint? attrs))
    ::literal-tag-and-inline-content

    ;; ; e.g. [:span x]
    (literal? tag)
    ::literal-tag

    ;; e.g. [x]
    :else
    ::default))

(declare compile-html)

(defmulti compile-element
  "Returns an unevaluated form that will render the supplied vector as a HTML
  element."
  {:private true}
  (fn [element env]
    (element-compile-strategy element env)))

(defmethod compile-element ::all-literal
  [element env]
  (compile-react-element (eval element) env))

(defmethod compile-element ::literal-tag-and-attributes
  [[tag attrs & content] env]
  (let [[tag attrs _] (normalize/element [tag attrs])]
    `(~(compile-constructor tag)
      ~(compile-tag tag)
      ~(compile-attrs attrs)
      ~@(map #(compile-html % env) content))))

(defmethod compile-element ::literal-tag-and-no-attributes
  [[tag & content] env]
  (compile-element (apply vector tag {} content) env))

(defmethod compile-element ::literal-tag-and-inline-content
  [[tag & content] env]
  (compile-element (apply vector tag {} content) env))

(defmethod compile-element ::literal-tag-and-hinted-attributes
  [[tag attrs & content] env]
  (let [[tag tag-attrs _] (normalize/element [tag])
        attrs-sym (gensym "attrs")]
    `(let [~attrs-sym ~attrs]
       (apply ~(compile-constructor tag)
              ~(compile-tag tag)
              ~(compile-merge-attrs tag-attrs attrs-sym)
              ~(when-not (empty? content)
                 (mapv #(compile-html % env) content))))))

(defmethod compile-element ::literal-tag
  [[tag attrs & content] env]
  (let [[tag tag-attrs _] (normalize/element [tag])
        attrs-sym (gensym "attrs")]
    `(let [~attrs-sym ~attrs]
       (apply ~(compile-constructor tag)
              ~(compile-tag tag)
              (if (map? ~attrs-sym)
                ~(compile-merge-attrs tag-attrs attrs-sym)
                ~(compile-attrs tag-attrs))
              (if (map? ~attrs-sym)
                ~(when-not (empty? content)
                   (mapv #(compile-html % env) content))
                ~(when attrs
                   (mapv #(compile-html % env) (cons attrs-sym content))))))))

(defmethod compile-element :default
  [element env]
  `(sablono.interpreter/interpret
    [~(first element)
     ~@(for [x (rest element)]
         (if (vector? x)
           (compile-element x env)
           x))]))

(defn compile-html
  "Pre-compile data structures into HTML where possible."
  ([content]
   (compile-html content nil))
  ([content env]
   (cond
     (vector? content) (compile-element content env)
     (literal? content) content
     (hint? content String) content
     (hint? content Number) content
     :else (compile-form content env))))

(extend-protocol ICompile
  clojure.lang.IPersistentVector
  (compile-react [this env]
    (if (element? this)
      (compile-react-element this env)
      (compile-react (seq this) env)))
  clojure.lang.ISeq
  (compile-react [this env]
    (map #(compile-react % env) this))
  Object
  (compile-react [this env]
    this)
  nil
  (compile-react [this env]
    nil))

(defn- to-js-map
  "Convert a map into a JavaScript object."
  [m]
  (if (every? literal? (keys m))
    (JSValue. (into {} (map (fn [[k v]] [k (to-js v)])) m))
    `(sablono.interpreter/attributes ~m)))

(extend-protocol IJSValue
  clojure.lang.Keyword
  (to-js [x]
    (name x))
  clojure.lang.PersistentArrayMap
  (to-js [x]
    (to-js-map x))
  clojure.lang.PersistentHashMap
  (to-js [x]
    (to-js-map x))
  clojure.lang.PersistentVector
  (to-js [x]
    (JSValue. (mapv to-js x)))
  Object
  (to-js [x]
    x)
  nil
  (to-js [_]
    nil))
