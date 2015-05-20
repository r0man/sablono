(ns sablono.interpreter-test
  (:require-macros [cemerick.cljs.test :refer [are is deftest testing]]
                   [sablono.test :refer [html-str]])
  (:require [cemerick.cljs.test :as t]
            [sablono.interpreter :as i]))

(deftest test-attributes
  (are [attrs expected]
    (is (= expected (js->clj (i/attributes attrs))))
    {} {}
    {:className ""} {}
    {:className "aa"} {"className" "aa"}
    {:className "aa bb"} {"className" "aa bb"}
    {:className ["aa bb"]} {"className" "aa bb"}))

(deftest test-interpret
  (are [form expected]
    (is (= expected (html-str (i/interpret form))))
    [:div] "<div></div>"
    [:div "x"] "<div>x</div>"
    [:div "1"] "<div>1</div>"
    [:div 1] "<div>1</div>"))

(deftest shorthand-div-forms
  (are [form expected]
    (is (= expected (html-str (i/interpret form))))
    [:#test.klass1] "<div id=\"test\" class=\"klass1\"></div>"))

(deftest test-static-children-as-arguments
  (testing "static children should interpret as direct React arguments"
    (is (= (html-str (i/interpret [:div [:div] [:div]]))
           "<div><div></div><div></div></div>"))))
