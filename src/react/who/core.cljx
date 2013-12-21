(ns react.who.core
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace]]
            #+clj [react.who.compiler :as compiler]))

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

(defn include-js
  "Include a list of external javascript files."
  [& scripts]
  (for [script scripts]
    [:script {:type "text/javascript", :src (str script)}]))

(defn include-css
  "Include a list of external stylesheet files."
  [& styles]
  (for [style styles]
    [:link {:type "text/css", :href (str style), :rel "stylesheet"}]))

(defn strip-react-attrs
  "Strip the React attributes from `s`."
  [s] (replace (str s) #"\s+data-reactid=\"[^\"]+\"" ""))

#+cljs
(defn render-dom [children]
  (let [body (aget (goog.dom/getElementsByTagNameAndClass "body") 0)
        container (goog.dom/createDom "div")
        id (gensym)]
    (goog.dom/append body container)
    (let [render-fn (fn [] (this-as this (js/React.DOM.div (clj->js {:id id}) children)))
          component (js/React.createClass #js {:render render-fn})]
      (js/React.renderComponent (component) container)
      (let [html (.-innerHTML (goog.dom/getElement (str id)))]
        (goog.dom/removeNode container)
        (strip-react-attrs html)))))
