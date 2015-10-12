(ns sablono.parser-test
  (:require [sablono.parser :as p]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [are deftest]])))

(deftest test-normalize-element
  (are [element expected]
      (= expected (p/normalize-element element))
    [:div]
    [:div nil]

    [:div {}]
    [:div {}]

    [:div.a]
    [:div.a nil]

    [:div {} [:a]]
    [:div {} [:a]]

    [:div {} [:a] [:b]]
    [:div {} [:a] [:b]]

    [:div [:a]]
    [:div nil [:a]]

    [:div [:a] [:b]]
    [:div nil [:a] [:b]]))

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

(deftest test-parse-element
  (are [element expected]
      (= expected (p/parse-element element))

    1 1
    1.0 1.0
    "x" "x"

    [:div]
    {:_isReactElement true
     :type "div"
     :props nil}

    [:#a]
    {:_isReactElement true
     :type "div"
     :props {:id "a"}}

    [ :.b]
    {:_isReactElement true
     :type "div"
     :props {:class #{"b"}}}

    [ :div#a]
    {:_isReactElement true
     :type "div"
     :props {:id "a"}}

    [ :div#a#a]
    {:_isReactElement true
     :type "div"
     :props {:id "a"}}

    [ :div.b]
    {:_isReactElement true
     :type "div"
     :props {:class #{"b"}}}

    [:div.b.b]
    {:_isReactElement true
     :type "div"
     :props {:class #{"b"}}}

    [ :div#a#b#c]
    {:_isReactElement true
     :type "div"
     :props {:id "a"}}

    [ :div.a.b.c]
    {:_isReactElement true
     :type "div"
     :props {:class #{"a" "b" "c"}}}

    [ :div#a.b]
    {:_isReactElement true
     :type "div"
     :props {:id "a" :class #{"b"}}}

    [ :div.b#a]
    {:_isReactElement true
     :type "div"
     :props {:id "a" :class #{"b"}}}

    [ :div#a#b#c.d.e.f]
    {:_isReactElement true
     :type "div"
     :props {:class #{"d" "f" "e"}, :id "a"}}

    [:div "a"]
    {:_isReactElement true
     :type "div"
     :props {:children ["a"]}}

    [:div "a" "b" "c" [:div "d"]]
    {:_isReactElement true
     :type "div"
     :props
     {:children
      ["a" "b" "c"
       {:_isReactElement true
        :type "div"
        :props {:children ["d"]}}]}}))
