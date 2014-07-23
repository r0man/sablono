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

#+cljs
(defn interpret-no-dynamic-children
  "Same as `i/interpret` except disallows calling React DOM functions
  with arrays as arguments. React will produce the same output for a
  single array argument with multiple children as it will for those
  children expanded into arguments. E.g., `React.DOM.div(null,
  [React.DOM.div(null), React.DOM.div(null)])` produces the same
  output as `React.DOM.div(null, React.DOM.div(null),
  React.DOM.div(null))`, but the former produces a warning about
  dynamic children and the latter does not. We'd like to check that
  certain forms passed to `i/interpret` don't result in calls like the
  latter, so we have to wrap `i/interpret` using this function."
  [form]
  (let [dom-fn i/dom-fn
        wrapped (fn [tag]
                  (let [f (dom-fn tag)]
                    (fn [props & children]
                      (if-let [bad-args (seq (filter #(array? %) children))]
                        (throw (js/Error.
                                   (str "some args to " tag
                                        " are arrays (dynamic children): "
                                        (pr-str bad-args))))
                        (apply f props children)))))]
    (with-redefs [i/dom-fn wrapped]
      (i/interpret form))))

#+cljs
(deftest test-static-children-as-arguments
  (testing "static children should interpret as direct React arguments"
    (is (interpret-no-dynamic-children [:div [:div] [:div]]))))
