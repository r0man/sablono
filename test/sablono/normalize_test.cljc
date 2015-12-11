(ns sablono.normalize-test
  (:require [sablono.normalize :as normalize]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [are deftest is]])))

(deftest test-compact-map
  (are [x expected]
      (= expected (normalize/compact-map x))
    nil nil
    {} {}
    {:x nil} {}
    {:x []} {}
    {:x ["x"]} {:x ["x"]}))

(deftest test-merge-with-class
  (are [maps expected]
      (= expected (apply normalize/merge-with-class maps))
    []
    nil
    [{:a 1} {:b 2}]
    {:a 1 :b 2}
    [{:a 1 :class :a} {:b 2 :class "b"} {:c 3 :class ["c"]}]
    {:a 1 :b 2 :c 3 :class #{"a" "b" "c"}}
    [{:a 1 :class :a} {:b 2 :class "b"} {:c 3 :class (seq ["c"])}]
    {:a 1 :b 2 :c 3 :class #{"a" "b" "c"}}
    ['{:a 1 :class #{"a"}} '{:b 2 :class #{(if true "b")}}]
    '{:a 1 :class #{"a" (if true "b")} :b 2}))

(deftest test-strip-css
  (are [x expected]
      (= expected (normalize/strip-css x))
    nil nil
    "" ""
    "foo" "foo"
    "#foo" "foo"
    ".foo" "foo"))

(deftest test-match-tag
  (are [tag expected]
      (= expected (normalize/match-tag tag))
    :div ["div" nil []]
    :div#foo ["div" "foo" []]
    :div#foo.bar ["div" "foo" ["bar"]]
    :div.bar#foo ["div" "foo" ["bar"]]
    :div#foo.bar.baz ["div" "foo" ["bar" "baz"]]
    :div.bar.baz#foo ["div" "foo" ["bar" "baz"]]
    :div.bar#foo.baz ["div" "foo" ["bar" "baz"]])
  (let [[tag id classes] (normalize/match-tag :div#foo.bar.baz)]
    (is (= "div" tag))
    (is (= "foo" id))
    (is (= ["bar" "baz"] classes))
    (is (vector? classes))))

(deftest test-normalize-class
  (are [class expected]
      (= expected (normalize/normalize-class class))
    nil nil
    :x #{"x"}
    "x" #{"x"}
    ["x"] #{"x"}
    [:x] #{"x"}
    '(if true "x") #{'(if true "x")}
    'x #{'x}))

(deftest test-attributes
  (are [attrs expected]
      (= expected (normalize/attributes attrs))
    nil nil
    {} {}
    {:class nil} {:class nil}
    {:class "x"} {:class #{"x"}}
    {:class #{"x"}} {:class #{"x"}}
    '{:class #{"x" (if true "y")}} '{:class #{(if true "y") "x"}}))

(deftest test-children
  (are [children expected]
      (= expected (normalize/children children))
    [] []
    1 1
    "x" "x"
    ["x"] ["x"]
    [["x"]] ["x"]
    [["x" "y"]] ["x" "y"]))

(deftest test-element
  (are [element expected]
      (= expected (normalize/element element))
    [:div] ["div" {} nil]
    [:div {:class nil}] ["div" {:class nil} nil]
    [:div#foo] ["div" {:id "foo"} nil]
    [:div.foo] ["div" {:class #{"foo"}} nil]
    [:div.a.b] ["div" {:class #{"a" "b"}} nil]
    [:div.a.b {:class "c"}] ["div" {:class #{"a" "b" "c"}} nil]
    [:div.a.b {:class nil}] ["div" {:class #{"a" "b"}} nil]))
