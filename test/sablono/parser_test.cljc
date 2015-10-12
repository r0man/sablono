(ns sablono.parser-test
  (:require [sablono.parser :as p]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [deftest]])))

;; (deftest test-normalize-element
;;   (are [element expected]
;;       (= expected (p/normalize-element element))
;;     [:div]
;;     [:div nil]

;;     [:div {}]
;;     [:div {}]

;;     [:div.a]
;;     [:div.a nil]

;;     [:div {} [:a]]
;;     [:div {} [:a]]

;;     [:div {} [:a] [:b]]
;;     [:div {} [:a] [:b]]

;;     [:div [:a]]
;;     [:div nil [:a]]

;;     [:div [:a] [:b]]
;;     [:div nil [:a] [:b]]))

(deftest test-parse-tag
  (are [tag expected]
      (= expected (p/parse-tag tag))
    :div
    {:type "div" :props nil}

    :#a
    {:type "div" :props {:id "a"}}

    :.b
    {:type "div" :props {:class #{"b"}}}

    :div#a
    {:type "div" :props {:id "a"}}

    :div#a#a
    {:type "div" :props {:id "a"}}

    :div.b
    {:type "div" :props {:class #{"b"}}}

    :div.b.b
    {:type "div" :props {:class #{"b"}}}

    :div#a#b#c
    {:type "div" :props {:id "a"}}

    :div.a.b.c
    {:type "div" :props {:class #{"a" "b" "c"}}}

    :div#a.b
    {:type "div" :props {:id "a" :class #{"b"}}}

    :div.b#a
    {:type "div" :props {:id "a" :class #{"b"}}}

    :div#a#b#c.d.e.f
    {:type "div" :props {:class #{"d" "f" "e"}, :id "a"}}))

;; (deftest test-parse-element
;;   #_(are [element expected]
;;         (= expected (p/parse-element element))
;;       [:div]
;;       nil))
