(ns react.who.core
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace]]
            [clojure.walk :refer [postwalk-replace]]
            [react.who.util :refer [to-str]]
            #+clj [react.who.compiler :as compiler])
  #+cljs (:require-macros [react.who.core :refer [defelem]]))

#+clj
(defmacro html
  "Render Clojure data structures via Facebook's React."
  [options & content]
  (apply react.who.compiler/compile-html options content))

#+clj
(defmacro html-expand
  "Returns the expanded HTML generation forms."
  [& forms]
  `(macroexpand `(html ~~@forms)))

(defmacro defhtml
  "Define a function, but wrap its output in an implicit html macro."
  [name & fdecl]
  (let [[fhead fbody] (split-with #(not (or (list? %) (vector? %))) fdecl)
        wrap-html (fn [[args & body]] `(~args (html ~@body)))]
    `(defn ~name
       ~@fhead
       ~@(if (vector? (first fbody))
           (wrap-html fbody)
           (map wrap-html fbody)))))

(defmacro with-base-url
  "Sets a base URL that will be prepended onto relative URIs. Note that for this
  to work correctly, it needs to be placed outside the html macro."
  [base-url & body]
  `(binding [react.who.util/*base-url* ~base-url]
     ~@body))

(defn wrap-attrs
  "Add an optional attribute argument to a function that returns a element vector."
  [func]
  (fn [& args]
    (if (map? (first args))
      (let [[tag & body] (apply func (rest args))]
        (if (map? (first body))
          (apply vector tag (merge (first body) (first args)) (rest body))
          (apply vector tag (first args) body)))
      (apply func args))))

(defn- update-arglists [arglists]
  (for [args arglists]
    (vec (cons 'attr-map? args))))

(defmacro defelem
  "Defines a function that will return a element vector. If the first argument
  passed to the resulting function is a map, it merges it with the attribute
  map of the returned element value."
  [name & fdecl]
  (let [fn-name# (gensym (str name))
        fdecl (postwalk-replace {name fn-name#} fdecl)]
    `(do (defn ~fn-name# ~@fdecl)
         (def ~name (react.who.core/wrap-attrs ~fn-name#)))))

(defn include-js
  "Include a list of external javascript files."
  [& scripts]
  (for [script scripts]
    [:script {:type "text/javascript", :src (react.who.util/to-str script)}]))

(defn include-css
  "Include a list of external stylesheet files."
  [& styles]
  (for [style styles]
    [:link {:type "text/css", :href (react.who.util/to-str style), :rel "stylesheet"}]))

(defn javascript-tag
  "Wrap the supplied javascript up in script tags and a CDATA section."
  [script]
  [:script {:type "text/javascript"}
   (str "//<![CDATA[\n" script "\n//]]>")])

(defelem link-to
  "Wraps some content in a HTML hyperlink with the supplied URL."
  [url & content]
  [:a {:href (react.who.util/to-str url)} content])

(defelem mail-to
  "Wraps some content in a HTML hyperlink with the supplied e-mail
  address. If no content provided use the e-mail address as content."
  [e-mail & [content]]
  [:a {:href (str "mailto:" e-mail)}
   (or content e-mail)])

(defelem unordered-list
  "Wrap a collection in an unordered list."
  [coll]
  [:ul (for [x coll] [:li x])])

(defelem ordered-list
  "Wrap a collection in an ordered list."
  [coll]
  [:ol (for [x coll] [:li x])])

(defelem image
  "Create an image element."
  ([src] [:img {:src (react.who.util/to-str src)}])
  ([src alt] [:img {:src (react.who.util/to-str src), :alt alt}]))
