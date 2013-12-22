(ns sablono.util-test
  (:import goog.Uri)
  (:require-macros [cemerick.cljs.test :refer [are is deftest testing]]
                   [sablono.core :refer [with-base-url]])
  (:require [cemerick.cljs.test :as t]
            [sablono.util :refer [as-str to-str to-uri]]))

(deftest test-as-str
  (are [args expected]
    (is (= expected (apply as-str args)))
    ["foo"] "foo"
    [:foo] "foo"
    [100] "100"
    ["a" :b 3] "ab3"
    [(Uri. "/foo")] "/foo"
    [(Uri. "localhost:3000/foo")] "localhost:3000/foo"))

(deftest test-to-uri
  (testing "with no base URL"
    (are [obj expected]
      (is (= expected (to-str (to-uri obj))))
      "foo" "foo"
      "/foo/bar" "/foo/bar"
      "/foo#bar" "/foo#bar"))
  (testing "with base URL"
    (with-base-url "/foo"
      (are [obj expected]
        (is (= expected (to-str (to-uri obj))))
        "/bar" "/foo/bar"
        "http://example.com" "http://example.com"
        "https://example.com/bar" "https://example.com/bar"
        "bar" "bar"
        "../bar" "../bar"
        "//example.com/bar" "//example.com/bar")))
  (testing "with base URL for root context"
    (with-base-url "/"
      (are [obj expected]
        (is (= expected (to-str (to-uri obj))))
        "/bar" "/bar"
        "http://example.com" "http://example.com"
        "https://example.com/bar" "https://example.com/bar"
        "bar" "bar"
        "../bar" "../bar"
        "//example.com/bar" "//example.com/bar")))
  (testing "with base URL containing trailing slash"
    (with-base-url "/foo/"
      (are [obj expected]
        (is (= expected (to-str (to-uri obj))))
        "/bar" "/foo/bar"
        "http://example.com" "http://example.com"
        "https://example.com/bar" "https://example.com/bar"
        "bar" "bar"
        "../bar" "../bar"
        "//example.com/bar" "//example.com/bar"))))
