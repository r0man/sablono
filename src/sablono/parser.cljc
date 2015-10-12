(ns sablono.parser
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(defn- strip
  "Strip the # and . characters from the beginning of `s`."
  [s]
  (some-> s (str/replace #"^[.#]" "")))

(defn- filter-first
  [c xs]
  (map strip (filter #(= c (first %1)) xs)))

(defn- find-classes
  [xs]
  (set (filter-first \. xs)))

(defn- find-id
  [xs]
  (first (set (filter-first \# xs))))

(defn- element?
  "Return true if `x` is a DOM element, otherwise false."
  [x]
  (and (vector? x)
       (keyword? (first x))))

(defn normalize-element [element]
  {:pre [(element? element)]}
  "Normalize `element` into a [type props & children] vector."
  (let [[type props & children] element]
    (vec (cond
           (map? props)
           element
           (and (nil? props) (empty? children))
           [type nil]
           :else
           (concat [type nil] (cons props children))))))

(defn parse-tag [tag]
  {:pre [(keyword? tag)]}
  "Parse the keyword `tag` as an HTML tag."
  (let [matches (re-seq #"[#.]?[^#.]+" (name tag))
        [type names]
        (cond
          ;; Not a valid tag.
          (empty? matches)
          (throw (ex-info (str "Can't parse tag: " tag) {:tag tag}))
          ;; Shorthand for div
          (#{\# \.} (ffirst matches))
          ["div" matches]
          :else
          [(first matches) (rest matches)])
        classes (find-classes names)
        id (find-id names)]
    {:type type
     :props
     (cond-> nil
       (not-empty classes)
       (assoc :class classes)
       id
       (assoc :id id))}))

(defn parse-element [element]
  "Parse the DOM `element`."
  {:pre [(vector? element)]}
  (let [[tag props & children] (normalize-element element)
        tag (parse-tag tag)]
    {:type (:type tag)
     :props
     (cond-> (merge (:props tag) props)
       (not-empty children)
       (assoc :children (vec children)))}))
