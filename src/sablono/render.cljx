(ns sablono.render
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [blank? join replace]]
            [clojure.walk :refer [postwalk]])
  #+clj (:import cljs.tagged_literals.JSValue))

(def ^{:doc "Regular expression that parses a CSS-style id and class from an element name." :private true}
  re-tag #"([^\s\.#]+)(?:#([^\s\.#]+))?(?:\.([^\s#]+))?")

(defprotocol HtmlRenderer
  (render-html [this] "Render a Clojure data structure via Facebook's React."))

(defn react-symbol [tag]
  (symbol "js" (str "React.DOM." (name tag))))

(defn- compact-map
  "Removes all map entries where value is nil."
  [m]
  (reduce
   (fn [m k]
     (if-let [v (get m k)]
       m (dissoc m k)))
   m (keys m)))

(defprotocol IJSValue
  (to-js [x]))

#+clj
(defn- to-js-map [x]
  (JSValue.
   (zipmap (map to-js (keys x))
           (map to-js (vals x)))))

#+clj
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
    x))

#+clj
(defn js-value [attrs]
  (to-js attrs))

(defn merge-with-class [& maps]
  (let [classes (->> (mapcat #(cond
                               (list? %1) [%1]
                               (vector? %1) %1
                               :else [%1])
                             (map :className maps))
                     (remove nil?) vec)
        maps (apply merge maps)]
    (if (empty? classes)
      maps (assoc maps :className classes))))

(defn normalize-element
  "Ensure an element vector is of the form [tag-name attrs content]."
  [[tag & content]]
  (when (not (or (keyword? tag) (symbol? tag) (string? tag)))
    (throw (ex-info (str tag " is not a valid element name.") {:tag tag :content content})))
  (let [[_ tag id class] (re-matches re-tag (name tag))
        tag-attrs {:id id :className (if class (replace class "." " "))}
        map-attrs (first content)]
    (if (map? map-attrs)
      [tag (compact-map (merge-with-class tag-attrs map-attrs)) (next content)]
      [tag (compact-map tag-attrs) content])))

#+clj
(defn render-attrs [attrs]
  (let [class (join " " (flatten (seq (:className attrs))))]
    (js-value (assoc attrs :className class))))

#+cljs
(defn render-attrs [attrs]
  (let [attrs (clj->js attrs)
        class (join " " (flatten (seq (.-className attrs))))]
    (if-not (blank? class)
      (set! (.-className attrs) class))
    attrs))

#+clj
(defn render-element
  "Render an element vector as a HTML element."
  [element]
  (let [[tag attrs content] (normalize-element element)]
    (if content
      `(~(react-symbol tag) (sablono.render/render-attrs ~(js-value attrs)) ~@(render-html content))
      `(~(react-symbol tag) (sablono.render/render-attrs ~(js-value attrs))))))

#+cljs
(defn render-element
  "Render an element vector as a HTML element."
  [element]
  (let [[tag attrs content] (normalize-element element)
        dom-fn (aget js/React.DOM (name tag))]
    (if content
      (dom-fn (render-attrs attrs) (render-html content))
      (dom-fn (render-attrs attrs)))))

(defn- render-seq [s]
  (into-array (map render-html s)))

#+clj
(extend-protocol HtmlRenderer
  clojure.lang.IPersistentVector
  (render-html [this]
    (render-element this))
  clojure.lang.ISeq
  (render-html [this]
    (map render-html this))
  Object
  (render-html [this]
    this)
  nil
  (render-html [this]
    nil))

#+cljs
(extend-protocol HtmlRenderer
  Cons
  (render-html [this]
    (render-seq this))
  ChunkedSeq
  (render-html [this]
    (render-seq this))
  LazySeq
  (render-html [this]
    (render-seq this))
  List
  (render-html [this]
    (render-seq this))
  IndexedSeq
  (render-html [this]
    (render-seq this))
  PersistentVector
  (render-html [this]
    (render-element this))
  default
  (render-html [this]
    this)
  nil
  (render-html [this]
    nil))
