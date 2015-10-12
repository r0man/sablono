(ns sablono.interpreter2
  (:require #?(:cljs cljsjs.react)
            [clojure.string :as str]
            [sablono.parser :as parser])
  #?(:clj (:import cljs.tagged_literals.JSValue)))

(defn- js-val [x]
  (some-> x #?(:clj (JSValue.) :cljs (clj->js))))

(defn- js-val [x]
  (if (coll? x)
    #?(:clj (JSValue. x)
       :cljs (clj->js x))
    x))

(defn- transform-class [element]
  (let [classes (-> element :props :class)]
    (if (set? classes)
      (-> (update-in element [:props] #(dissoc % :class))
          (assoc-in [:props :className] (str/join " " classes)))
      element)))

(defn- transform-children [element]
  (if (-> element :props :children)
    (update-in element [:props :children] #(js-val (map js-val %)))
    element))

(defn- transform-props [element]
  (if (:props element)
    (update-in element [:props] js-val)
    element))

(defn- transform-element [element]
  (-> element
      transform-class
      transform-children
      transform-props
      js-val))

(defn html [element]
  (-> element parser/parse-element transform-element))

;; (html [:div])
;; (html [:div "a"])
;; (html [:div "a" [:b]])
;; (html [:div "a" "b" [:div "c"]])

;; (js/React.renderToString (html [:div]))
