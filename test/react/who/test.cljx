(ns react.who.test
  (:require [react.who.core :as core]))

(defmacro html
  "Render Clojure data structures to a string of HTML."
  [options & content]
  `(let [body# (aget (goog.dom/getElementsByTagNameAndClass "body") 0)]
     (goog.dom/removeChildren body#)
     (js/React.renderComponent (core/html ~options ~@content) body#)
     (clojure.string/replace (.-innerHTML body#) #"\s+data-reactid=\"[^\"]+\"" "")))

(defmacro are-html-rendered [& body]
  `(cemerick.cljs.test/are [form# expected#]
     (cemerick.cljs.test/is (= expected# (react.who.test/html form#)))
     ~@body))
