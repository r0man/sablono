(ns react.who.test
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace]]
            [react.who.core :as core]))

(defmacro are-html-rendered [& body]
  `(cemerick.cljs.test/are [form# expected#]
     (cemerick.cljs.test/is (= expected# (react.who.test/render-dom (react.who.core/html form#))))
     ~@body))

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
        (react.who.test/strip-react-attrs html)))))
