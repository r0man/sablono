(ns sablono.generators
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test.check.generators :as gen]))

(def html-types
  (gen/elements #{:div :a}))

(def html-attributes
  (gen/map gen/keyword gen/string-alphanumeric))

(defn element-with-attributes
  [child-gen]
  (gen/tuple html-types html-attributes child-gen))

(defn element-without-attributes
  [child-gen]
  (gen/tuple html-types child-gen))

(defn container
  [child-gen]
  (gen/one-of [(element-with-attributes child-gen)
               (element-without-attributes child-gen)]))

(def children
  (gen/one-of [gen/string-alphanumeric gen/int]))

(def elements
  (gen/recursive-gen container children))

(comment
  (pprint (gen/sample elements 10))
  (pprint (last (gen/sample elements 20))))
