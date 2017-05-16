(ns sablono.test
  (:require [cljs.compiler :as cljs]
            [clojure.set :as set]
            [clojure.test.check]
            [clojure.test.check.generators]
            [clojure.walk :refer [prewalk]]
            [clojure.xml :as xml]
            [sablono.core]
            [om.dom :as dom])
  (:import cljs.tagged_literals.JSValue
           java.io.StringBufferInputStream))

(defmacro html-str [element]
  `(sablono.server/render-static
    (sablono.core/html ~element)))

(defmacro html-data [element]
  `(some-> (sablono.test/html-str ~element)
           (tubax.core/xml->clj)))

;; Because instances of cljs.tagged_literals.JSValue are not
;; comparable via = we define our own version of JSValue and use this
;; with the reader literal #j

(deftype JSWrapper [val]
  Object
  (equals [this other]
    (and (instance? JSWrapper other)
         (= (.val this) (.val other))))
  (hashCode [this]
    (.hashCode (.val this)))
  (toString [this]
    (.toString (.val this))))

(defn print-js-value
  [prefix value writer]
  (.write writer (str prefix " "))
  (.write writer (pr-str (.val value))))

(defmethod print-method JSWrapper
  [value writer]
  (print-js-value "#jw" value writer))

(defmethod print-method JSValue
  [value writer]
  (print-js-value "#js" value writer))

(defn js-value? [x]
  (instance? JSValue x))

(defn- replace-js-value [forms]
  (prewalk
   (fn [form]
     (if (js-value? form)
       (JSWrapper. (replace-js-value (.val form))) form))
   forms))

(defn replace-gensyms [forms]
  (prewalk
   (fn [form]
     (if (and (symbol? form)
              (re-matches #"attrs\d+" (str form)))
       'attrs form))
   forms))

(defn ===
  "Same as clojure.core/=, but replaces `cljs.tagged_literals.JSValue`
  with instances of `sablono.js-value.JSWrapper` (so we can compare
  with =), and strips of numbers from gensyms before comparison."
  [& more]
  (->> (map replace-js-value more)
       (map replace-gensyms)
       (apply =)))

(defn- xml-map? [x]
  (and (map? x) (= (keys x) [:tag :attrs :content])))

(defn- reshape [data]
  (prewalk
   (fn [data]
     (if (xml-map? data)
       (-> (set/rename-keys (into {} data) {:attrs :attributes})
           (update :attributes dissoc
                   :data-react-checksum
                   :data-reactid
                   :data-reactroot)
           (update :content vec))
       data))
   data))

(defn parse-xml [s]
  (-> (StringBufferInputStream. s)
      (xml/parse)
      (reshape)))

(defn render-str [x]
  (dom/render-to-str x))
