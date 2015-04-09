(ns sablono.util-test
  #?(:cljs (:require-macros
            [cemerick.cljs.test :refer [are is deftest testing]]
            [sablono.core :refer [with-base-url]]))
  #?(:cljs (:import goog.Uri))
  (:require [sablono.util :as u]
            #?(:clj [clojure.test :refer :all])
            #?(:cljs [cemerick.cljs.test :as t])))

(deftest test-camel-case-keys
  (are [attrs expected]
    (= expected (u/camel-case-keys attrs))
    {:id "x"}
    {:id "x"}
    {:class "x"}
    {:class "x"}
    {:http-equiv "Expires"}
    {:httpEquiv "Expires"}
    {:style {:z-index 1000}}
    {:style {:zIndex 1000}}
    {:on-click '(fn [e] (let [m {:a-b "c"}]))}
    {:onClick '(fn [e] (let [m {:a-b "c"}]))}))

(deftest test-html-to-dom-attrs
  (are [attrs expected]
    (= expected (u/html-to-dom-attrs attrs))
    {:id "x"}
    {:id "x"}
    {:class "x"}
    {:className "x"}
    {:http-equiv "Expires"}
    {:httpEquiv "Expires"}
    {:style {:z-index 1000}}
    {:style {:zIndex 1000}}
    {:on-click '(fn [e] (let [m {:a-b "c"}]))}
    {:onClick '(fn [e] (let [m {:a-b "c"}]))}))

(deftest test-compact-map
  (are [x expected]
    (is (= expected (u/compact-map x)))
    nil nil
    {} {}
    {:x nil} {}
    {:x []} {}
    {:x ["x"]} {:x ["x"]}))

(deftest test-merge-with-class
  (are [maps expected]
    (is (= expected (apply u/merge-with-class maps)))
    [] nil
    [{:a 1} {:b 2}]
    {:a 1 :b 2}
    [{:a 1 :class :a} {:b 2 :class "b"} {:c 3 :class ["c"]}]
    {:a 1 :b 2 :c 3 :class [:a "b" "c"]}
    [{:a 1 :class :a} {:b 2 :class "b"} {:c 3 :class (seq ["c"])}]
    {:a 1 :b 2 :c 3 :class [:a "b" "c"]}))

(deftest test-strip-css
  (are [x expected]
    (is (= expected (u/strip-css x)))
    nil nil
    "" ""
    "foo" "foo"
    "#foo" "foo"
    ".foo" "foo"))

(deftest test-match-tag
  (are [tag expected]
    (is (= expected (u/match-tag tag)))
    :div ["div" nil []]
    :div#foo ["div" "foo" []]
    :div#foo.bar ["div" "foo" ["bar"]]
    :div.bar#foo ["div" "foo" ["bar"]]
    :div#foo.bar.baz ["div" "foo" ["bar" "baz"]]
    :div.bar.baz#foo ["div" "foo" ["bar" "baz"]]
    :div.bar#foo.baz ["div" "foo" ["bar" "baz"]])
  (let [[tag id classes] (u/match-tag :div#foo.bar.baz)]
    (is (= "div" tag))
    (is (= "foo" id))
    (is (= ["bar" "baz"] classes))
    (is (vector? classes))))

(deftest test-normalize-element
  (are [element expected]
    (is (= expected (u/normalize-element element)))
    [:div] ["div" {} nil]
    [:div {:class nil}] ["div" {:class nil} nil]
    [:div#foo] ["div" {:id "foo"} nil]
    [:div.foo] ["div" {:class ["foo"]} nil]
    [:div.a.b] ["div" {:class ["a" "b"]} nil]
    [:div.a.b {:class "c"}] ["div" {:class ["a" "b" "c"]} nil]
    [:div.a.b {:class nil}] ["div" {:class ["a" "b"]} nil])
  (let [[tag attrs & content] (u/normalize-element [:div.foo])]
    (is (= "div" tag))
    (is (= {:class ["foo"]} attrs))
    (is (vector? (:class attrs)))))

#?(:cljs
   (deftest test-as-str
     (are [args expected]
       (is (= expected (apply u/as-str args)))
       ["foo"] "foo"
       [:foo] "foo"
       [100] "100"
       ["a" :b 3] "ab3"
       [(Uri. "/foo")] "/foo"
       [(Uri. "localhost:3000/foo")] "localhost:3000/foo")))

(deftest test-camel-case
  (are [attr expected]
    (is (= expected (u/camel-case attr)))
    nil nil
    "" ""
    :data :data
    :data-toggle :data-toggle
    :http-equiv :httpEquiv
    :aria-checked :aria-checked))

#?(:cljs
   (deftest test-to-uri
     (testing "with no base URL"
       (are [obj expected]
         (is (= expected (u/to-str (u/to-uri obj))))
         "foo" "foo"
         "/foo/bar" "/foo/bar"
         "/foo#bar" "/foo#bar"))
     (testing "with base URL"
       (with-base-url "/foo"
         (are [obj expected]
           (is (= expected (u/to-str (u/to-uri obj))))
           "/bar" "/foo/bar"
           "http://example.com" "http://example.com"
           "https://example.com/bar" "https://example.com/bar"
           "bar" "bar"
           "../bar" "../bar"
           "//example.com/bar" "//example.com/bar")))
     (testing "with base URL for root context"
       (with-base-url "/"
         (are [obj expected]
           (is (= expected (u/to-str (u/to-uri obj))))
           "/bar" "/bar"
           "http://example.com" "http://example.com"
           "https://example.com/bar" "https://example.com/bar"
           "bar" "bar"
           "../bar" "../bar"
           "//example.com/bar" "//example.com/bar")))
     (testing "with base URL containing trailing slash"
       (with-base-url "/foo/"
         (are [obj expected]
           (is (= expected (u/to-str (u/to-uri obj))))
           "/bar" "/foo/bar"
           "http://example.com" "http://example.com"
           "https://example.com/bar" "https://example.com/bar"
           "bar" "bar"
           "../bar" "../bar"
           "//example.com/bar" "//example.com/bar")))))

(deftest test-strip-attr
  (are [html attr expected]
    (is (= expected (u/strip-attr html attr)))
    nil :data-reactid nil
    "" :data-reactid ""
    "<div></div>" :data-reactid "<div></div>"
    "<div data-reactid=\"1\"></div>" :data-reactid "<div></div>"
    "<div data-reactid='1'></div>" :data-reactid "<div></div>"
    "<div data-reactid=\"1\" data-checksum=\"2\"></div>" :data-reactid "<div data-checksum=\"2\"></div>"
    "<div data-reactid='1' data-checksum='2'></div>" :data-reactid "<div data-checksum='2'></div>"))

(deftest test-strip-outer
  (are [html expected]
    (is (= expected (u/strip-outer html)))
    nil nil
    "" ""
    "<div>x</div>" "x"
    "<div><div>x</div></div>" "<div>x</div>"
    " <div id=\"a\">\n<div>x</div></div> " "<div>x</div>"))
