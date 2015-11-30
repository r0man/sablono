(ns sablono.interpreter-test
  (:require-macros [cljs.test :refer [are is deftest testing]]
                   [sablono.test :refer [html-str]])
  (:require [cljs.test :as t]
            [sablono.core :as c]
            [sablono.interpreter :as i]))

(defn interpret [x]
  (some->> (i/interpret x)
           (c/render-static)
           (hickory.core/parse-fragment)
           (map hickory.core/as-hiccup)
           (first)))

(deftest test-attributes
  (are [attrs expected]
      (is (= expected (js->clj (i/attributes attrs))))
    {} {}
    {:className ""} {}
    {:className "aa"} {"className" "aa"}
    {:className "aa bb"} {"className" "aa bb"}
    {:className ["aa bb"]} {"className" "aa bb"}))

(deftest test-interpret-shorthand-div-forms
  (is (= (interpret [:#test.klass1])
         [:div {:id "test" :class "klass1"}])))

(deftest test-interpret-static-children-as-arguments
  (is (= (interpret
          [:div
           [:div {:class "1" :key 1}]
           [:div {:class "2" :key 2}]])
         [:div {}
          [:div {:class "1"}]
          [:div {:class "2"}]])))

(deftest test-interpret-div
  (is (= (interpret [:div])
         [:div {}])))

(deftest test-interpret-div-with-string
  (is (= (interpret [:div "x"])
         [:div {} "x"])))

(deftest test-interpret-div-with-number
  (is (= (interpret [:div 1])
         [:div {} "1"])))

(deftest test-interpret-div-with-nested-lazy-seq
  (is (= (interpret [:div (map identity ["A" "B"])])
         [:div {} "AB"])))

(deftest test-interpret-div-with-nested-list
  (is (= (interpret [:div (list "A" "B")])
         [:div {} "AB"])))

(deftest test-interpret-div-with-nested-vector
  (is (= (interpret [:div ["A" "B"]])
         [:div {} "AB"]))
  (is (= (interpret [:div (vector"A" "B")])
         [:div {} "AB"])))
