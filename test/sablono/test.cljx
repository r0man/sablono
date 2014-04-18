(ns sablono.test
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [replace]]
            [sablono.core :as core]
            #+cljs [goog.dom :as dom]))

(defmacro are-html-rendered [& body]
  `(cemerick.cljs.test/are [form# expected#]
     (cemerick.cljs.test/is (= expected# (sablono.test/render-dom (sablono.core/html form#))))
     ~@body))

(defmacro html-str [& contents]
  `(sablono.test/render-dom (sablono.core/html ~@contents)))

#+cljs
(defn body []
  (aget (goog.dom/getElementsByTagNameAndClass "body") 0))

#+cljs
(defn render-dom [children]
  (let [container (goog.dom/createDom "div")
        id (gensym)]
    (goog.dom/append (body) container)
    (let [render-fn (fn [] (this-as this (js/React.DOM.div (clj->js {:id id}) children)))
          component (js/React.createClass #js {:render render-fn})]
      (js/React.renderComponent (component) container)
      (let [html (.-innerHTML (goog.dom/getElement (str id)))]
        (goog.dom/removeNode container)
        (sablono.util/strip-attr html :data-reactid)))))
