(ns sablono.core
  (:require [clojure.walk :refer [postwalk-replace]]
            [sablono.interpreter :as interpreter]
            [sablono.compiler :as compiler]))

(defn- cljs-env?
  "Take the &env from a macro, and tell whether we are expanding into cljs."
  [env]
  (boolean (:ns env)))

(defmacro attrs
  "Compile `attributes` map into a JavaScript literal."
  [attributes]
  (compiler/compile-attrs attributes))

(defmacro html*
  "Compile the Hiccup `form`. Always produces code that evaluates to
  React elements."
  [form]
  (compiler/compile-html form &env))

(defmacro html
  "Compile the Hiccup `form`. Produces code that evaluates to React
  elements when running under ClojureScript environment, or
  om.dom.Element records when running under Clojure."
  [form]
  (if (cljs-env? &env)
    (compiler/compile-html form &env)
    `(interpreter/interpret ~form)))

(defmacro html-expand
  "Macro expand the Hiccup `content`."
  [form]
  `(macroexpand `(html ~~form)))

(defmacro defhtml
  "Define a function, but wrap its output in an implicit html macro."
  [name & fdecl]
  (let [[fhead fbody] (split-with #(not (or (list? %) (vector? %))) fdecl)
        wrap-html (fn [[args & body]] `(~args (html ~@body)))
        tag (if (cljs-env? &env) 'js/React.Element 'sablono.html.Element)
        name (vary-meta name assoc :tag tag)]
    `(defn ~name
       ~@fhead
       ~@(if (vector? (first fbody))
           (wrap-html fbody)
           (map wrap-html fbody)))))

(defmacro with-base-url
  "Sets a base URL that will be prepended onto relative URIs. Note
  that for this to work correctly, it needs to be placed outside the
  html macro."
  [base-url & body]
  `(binding [sablono.util/*base-url* ~base-url]
     ~@body))

(defmacro defelem
  "Defines a function that will return a element vector. If the first
  argument passed to the resulting function is a map, it merges it
  with the attribute map of the returned element value."
  [name & fdecl]
  (let [fn-name# (gensym (str name))
        fdecl (postwalk-replace {name fn-name#} fdecl)]
    `(do (defn ~fn-name# ~@fdecl)
         (def ~name (sablono.core/wrap-attrs ~fn-name#)))))

(defmacro with-group
  "Group together a set of related form fields."
  [group & body]
  `(binding [sablono.core/*group*
             (conj sablono.core/*group*
                   (sablono.util/as-str ~group))]
     (list ~@body)))

(defn gen-input-field
  "Generate an input field of `input-type`."
  [input-type]
  (let [fn-name (symbol (str input-type "-field"))
        docstring (str "Creates a " input-type " input field.")]
    `(defelem ~fn-name
       ~docstring
       ([name#]
        (sablono.core/input-field* (str '~input-type) name#))
       ([name# value#]
        (sablono.core/input-field* (str '~input-type) name# value#)))))

(def input-fields
  "The types of HTML input fields."
  '[color date datetime datetime-local email file hidden month
    number password range search tel text time url week])

(defmacro gen-input-fields
  "Generate input fields for all `input-fields`."
  []
  `(do ~@(map gen-input-field input-fields)))
