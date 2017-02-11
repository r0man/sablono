(ns sablono.specs-test
  (:require [clojure.spec :as s]
            [clojure.test :refer [deftest is]]
            [sablono.specs :as specs]))

(deftest execise-tag
  (is (s/exercise ::specs/tag)))

(deftest execise-attribute
  (is (s/exercise ::specs/attribute)))

(deftest execise-children
  (is (s/exercise ::specs/children)))

(deftest execise-element
  (is (s/exercise ::specs/element)))
