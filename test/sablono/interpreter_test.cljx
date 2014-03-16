(ns sablono.interpreter-test
  #+cljs (:require-macros [cemerick.cljs.test :refer [are is deftest testing]])
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
