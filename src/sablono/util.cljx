(ns sablono.util
  #+cljs (:import goog.Uri)
  (:require [clojure.string :refer [split]]))

(def ^:dynamic *base-url* nil)

(def ^{:doc "Regular expression that parses a CSS-style id and class from an element name." :private true}
  re-tag #"([^\s\.#]+)(?:#([^\s\.#]+))?(?:\.([^\s#]+))?")

(defprotocol ToString
  (to-str [x] "Convert a value into a string."))

(defprotocol ToURI
  (to-uri [x] "Convert a value into a URI."))

(defn as-str
  "Converts its arguments into a string using to-str."
  [& xs]
  (apply str (map to-str xs)))

(defn compact-map
  "Removes all map entries where value is nil."
  [m]
  (reduce
   (fn [m k]
     (if-let [v (get m k)]
       m (dissoc m k)))
   m (keys m)))

(defn merge-with-class
  "Like clojure.core/merge but concat :className entries."
  [& maps]
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
        tag-attrs {:id id :className (if class (split class #"\."))}
        map-attrs (first content)]
    (if (map? map-attrs)
      [tag (compact-map (merge-with-class tag-attrs map-attrs)) (next content)]
      [tag (compact-map tag-attrs) content])))

#+cljs
(extend-protocol ToString
  cljs.core.Keyword
  (to-str [x]
    (name x))
  goog.Uri
  (to-str [x]
    (if (or (. x (hasDomain))
            (nil? (. x (getPath)))
            (not (re-matches #"^/.*" (. x (getPath)))))
      (str x)
      (let [base (str *base-url*)]
        (if (re-matches #".*/$" base)
          (str (subs base 0 (dec (count base))) x)
          (str base x)))))
  nil
  (to-str [_]
    "")
  number
  (to-str [x]
    (str x))
  default
  (to-str [x]
    (str x)))

#+cljs
(extend-protocol ToURI
  Uri
  (to-uri [x] x)
  default
  (to-uri [x] (Uri. (str x))))
