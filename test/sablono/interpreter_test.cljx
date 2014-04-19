(ns sablono.interpreter-test
  #+cljs (:require-macros [cemerick.cljs.test :refer [are is deftest testing]]
                          [sablono.test :refer [html-str]])
  (:require [sablono.interpreter :as i]
            #+cljs [cemerick.cljs.test :as t]))

#+cljs
(deftest test-attributes
  (are [attrs expected]
    (is (= expected (js->clj (i/attributes attrs))))
    {} {}
    {:className ""} {}
    {:className "aa"} {"className" "aa"}
    {:className "aa bb"} {"className" "aa bb"}
    {:className ["aa bb"]} {"className" "aa bb"}))

#+cljs
(deftest test-interpret
  (are [form expected]
    (is (= expected (html-str (i/interpret form))))
    [:div] "<div></div>"
    [:div "x"] "<div>x</div>"
    [:div "1"] "<div>1</div>"
    [:div 1] "<div>1</div>"))

#+cljs
(deftest shorthand-div-forms
  (are [form expected]
    (is (= expected (html-str (i/interpret form))))
    [:#test.klass1] "<div id=\"test\" class=\"klass1\"></div>"))
