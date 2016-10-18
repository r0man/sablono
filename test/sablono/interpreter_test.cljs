(ns sablono.interpreter-test
  (:require-macros [sablono.test :refer [html-str]])
  (:require [clojure.test :refer [are is testing]]
            [devcards.core :refer-macros [deftest]]
            [sablono.core :as c]
            [sablono.interpreter :as i]
            [sablono.server :as server]
            [tubax.core :refer [xml->clj]]))

(defn interpret
  "Interpret `x` as a Hiccup data structure, render it as a static
  HTML string, parse it and return a Clojure data structure."
  [x]
  (some->> (i/interpret x)
           (server/render-static)
           (xml->clj)))

(deftest test-attributes
  (are [attrs expected]
      (= expected (js->clj (i/attributes attrs)))
    {} {}
    {:className ""} {}
    {:className "aa"}       {"className" "aa"}
    {:className "aa bb"}    {"className" "aa bb"}
    {:className ["aa bb"]}  {"className" "aa bb"}
    {:className '("aa bb")} {"className" "aa bb"}
    {:id :XY}               {"id" "XY"}))

(deftest test-interpret-shorthand-div-forms
  (is (= (interpret [:#test.klass1])
         {:tag :div
          :attributes {:id "test" :class "klass1"}
          :content []})))

(deftest test-interpret-static-children-as-arguments
  (is (= (interpret
          [:div
           [:div {:class "1" :key 1}]
           [:div {:class "2" :key 2}]])
         {:tag :div
          :attributes {}
          :content
          [{:tag :div :attributes {:class "1"} :content []}
           {:tag :div :attributes {:class "2"} :content []}]})))

(deftest test-interpret-div
  (is (= (interpret [:div])
         {:tag :div
          :attributes {}
          :content []})))

(deftest test-interpret-div-with-string
  (is (= (interpret [:div "x"])
         {:tag :div
          :attributes {}
          :content ["x"]})))

(deftest test-interpret-div-with-number
  (is (= (interpret [:div 1])
         {:tag :div
          :attributes {}
          :content ["1"]})))

(deftest test-interpret-div-with-nested-lazy-seq
  (is (= (interpret [:div (map identity ["A" "B"])])
         {:tag :div
          :attributes {}
          :content ["AB"]})))

(deftest test-interpret-div-with-nested-list
  (is (= (interpret [:div (list "A" "B")])
         {:tag :div
          :attributes {}
          :content ["AB"]})))

(deftest test-interpret-div-with-nested-vector
  (is (= (interpret [:div ["A" "B"]])
         {:tag :div
          :attributes {}
          :content ["AB"]})))

(deftest test-class-duplication
  (is (= (interpret [:div.a.a.b.b.c {:class "c"}])
         {:tag :div
          :attributes {:class "a a b b c c"}
          :content []}))  )

(deftest test-class-as-set
  (is (= (interpret [:div {:class #{"a" "b" "c"}}])
         {:tag :div
          :attributes {:class "a b c"}
          :content []})))

(deftest test-class-as-list
  (is (= (interpret [:div {:class (list "a" "b" "c")}])
         {:tag :div
          :attributes {:class "a b c"}
          :content []})))

(deftest test-class-as-vector
  (is (= (interpret [:div {:class (vector "a" "b" "c")}])
         {:tag :div
          :attributes {:class "a b c"}
          :content []})))

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
         {:tag :div
          :attributes {}
          :content
          [{:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}]})))

(deftest test-issue-90
  (is (= (interpret [:div nil (case :a :a "a")])
         {:tag :div
          :attributes {}
          :content ["a"]})))

(deftest test-issue-57
  (let [payload {:username "john" :likes 2}]
    (is (= (interpret
            (let [{:keys [username likes]} payload]
              [:div
               [:div (str username " (" likes ")")]
               [:div "!Pixel Scout"]]))
           {:tag :div
            :attributes {}
            :content
            [{:tag :div
              :attributes {}
              :content ["john (2)"]}
             {:tag :div
              :attributes {}
              :content ["!Pixel Scout"]}]}))))
