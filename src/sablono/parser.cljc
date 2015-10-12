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

(defn element?
  "Return true if `x` is a DOM element, otherwise false."
  [x]
  (and (vector? x)
       (keyword? (first x))))

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

;; (parse-tag :#a)

;; (declare parse-element)

;; (defn merge-props
;;   "Like clojure.core/merge but concatenate :class entries."
;;   [& props]
;;   (apply merge-with
;;          (fn [v1 v2]
;;            (cond
;;              (or (set? v1) (set? v2))
;;              (set/union v1 v2)
;;              :else v2))
;;          props))

;; (defn- parse-children
;;   "Parse the `children` of a DOM element."
;;   [children]
;;   (map (fn [child]
;;          (if (element? child)
;;            (parse-element child)
;;            child))
;;        children))

;; (defn parse-element [element]
;;   "Parse the DOM `element`."
;;   {:pre [(vector? element)]}
;;   (let [[tag props-1 & children] (normalize-element element)
;;         props-1 (normalize-props props-1)
;;         [tag props-2] (parse-tag tag)]
;;     (vec (concat [tag (merge-props props-1 props-2)]
;;                  (parse-children children)))))

;; ;; (parse-element [:div#a.b {:class "c"} [:div.a]])
;; ;; (parse-element '[:div#a.b {:class "c"} ((fn [] [:div.a]))])
;; ;; (parse-element [:div#a.b {:class #{"c"}}])
;; ;; (parse-element [:div#a.b {:class ["c"]}])
;; ;; (merge-props {:class #{"c"}} {:class #{"b"}, :id "a"})

;; ;; (parse-element [:div#id.class [:a.link [:div]]])
;; ;; (parse-element [:div#id.class {} [:a.link]])
