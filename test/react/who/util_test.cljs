(ns react.who.util-test
  (:import goog.Uri)
  (:require-macros [cemerick.cljs.test :refer [is deftest testing]]
                   [react.who.core :refer [with-base-url]])
  (:require [cemerick.cljs.test :as t]
            [react.who.util :refer [as-str to-str to-uri]]))

(deftest test-as-str
  (is (= (as-str "foo") "foo"))
  (is (= (as-str :foo) "foo"))
  (is (= (as-str 100) "100"))
  ;; (is (= (as-str 4/3) (str (float 4/3))))
  (is (= (as-str "a" :b 3) "ab3"))
  (is (= (as-str (Uri. "/foo")) "/foo"))
  (is (= (as-str (Uri. "localhost:3000/foo")) "localhost:3000/foo")))

(deftest test-to-uri
  (testing "with no base URL"
    (is (= (to-str (to-uri "foo")) "foo"))
    (is (= (to-str (to-uri "/foo/bar")) "/foo/bar"))
    (is (= (to-str (to-uri "/foo#bar")) "/foo#bar")))
  (testing "with base URL"
    (with-base-url "/foo"
      (is (= (to-str (to-uri "/bar")) "/foo/bar"))
      (is (= (to-str (to-uri "http://example.com")) "http://example.com"))
      (is (= (to-str (to-uri "https://example.com/bar")) "https://example.com/bar"))
      (is (= (to-str (to-uri "bar")) "bar"))
      (is (= (to-str (to-uri "../bar")) "../bar"))
      (is (= (to-str (to-uri "//example.com/bar")) "//example.com/bar"))))
  (testing "with base URL for root context"
    (with-base-url "/"
      (is (= (to-str (to-uri "/bar")) "/bar"))
      (is (= (to-str (to-uri "http://example.com")) "http://example.com"))
      (is (= (to-str (to-uri "https://example.com/bar")) "https://example.com/bar"))
      (is (= (to-str (to-uri "bar")) "bar"))
      (is (= (to-str (to-uri "../bar")) "../bar"))
      (is (= (to-str (to-uri "//example.com/bar")) "//example.com/bar"))))
  (testing "with base URL containing trailing slash"
    (with-base-url "/foo/"
      (is (= (to-str (to-uri "/bar")) "/foo/bar"))
      (is (= (to-str (to-uri "http://example.com")) "http://example.com"))
      (is (= (to-str (to-uri "https://example.com/bar")) "https://example.com/bar"))
      (is (= (to-str (to-uri "bar")) "bar"))
      (is (= (to-str (to-uri "../bar")) "../bar"))
      (is (= (to-str (to-uri "//example.com/bar")) "//example.com/bar")))))
