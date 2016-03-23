(ns sablono.interpreter-test
  (:require-macros [cljs.test :refer [are is testing]]
                   [sablono.test :refer [html-str]])
  (:require [cljs.test :as t]
            [devcards.core :refer-macros [deftest]]
            [sablono.core :as c]
            [sablono.interpreter :as i]
            [sablono.server :as server]))

(defn interpret [x]
  (some->> (i/interpret x)
           (server/render-static)
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
    {:className ["aa bb"]} {"className" "aa bb"}
    {:className '("aa bb")} {"className" "aa bb"}
    {:id :XY} {"id" "XY"}))

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

(deftest test-class-duplication
  (is (= (interpret [:div.a.a.b.b.c {:class "c"}])
         [:div {:class "a a b b c c"}]))  )

(deftest test-class-as-set
  (is (= (interpret [:div {:class #{"a" "b" "c"}}])
         [:div {:class "a b c"}])))

(deftest test-class-as-list
  (is (= (interpret [:div {:class (list "a" "b" "c")}])
         [:div {:class "a b c"}])))

(deftest test-class-as-vector
  (is (= (interpret [:div {:class (vector "a" "b" "c")}])
         [:div {:class "a b c"}])))

(deftest test-issue-80
  (is (= (interpret
          [:div
           [:div {:class (list "foo" "bar")}]
           [:div {:class (vector "foo" "bar")}]
           (let []
             [:div {:class (list "foo" "bar")}])
           (let []
             [:div {:class (vector "foo" "bar")}])
           (when true
             [:div {:class (list "foo" "bar")}])
           (when true
             [:div {:class (vector "foo" "bar")}])
           (do
             [:div {:class (list "foo" "bar")}])
           (do
             [:div {:class (vector "foo" "bar")}])])
         [:div {}
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]])))

(deftest test-issue-90
  (is (= (interpret [:div nil (case :a :a "a")])
         [:div {} "a"])))

(deftest test-issue-57
  (let [payload {:username "john" :likes 2}]
    (is (= (interpret
            (let [{:keys [username likes]} payload]
              [:div
               [:div (str username " (" likes ")")]
               [:div "!Pixel Scout"]]))
           [:div {}
            [:div {} "john (2)"]
            [:div {} "!Pixel Scout"]]))))
