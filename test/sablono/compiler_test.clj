(ns sablono.compiler-test
  (:refer-clojure :exclude [compile])
  (:require [clojure.spec.alpha :as s]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [sablono.compiler :refer :all]
            [sablono.core :refer [attrs html* html-expand]]
            [sablono.interpreter :as interpreter]
            [sablono.test :refer [=== js-value?]]))

(defmacro compile [form]
  `(macroexpand '(html* ~form)))

(defmacro are-html [& body]
  `(are [form# expected#]
       (=== (macroexpand `(html* ~form#))
            expected#)
     ~@body))

(deftest test-compile-attrs
  (are [attrs expected] (=== (compile-attrs attrs) expected)
    nil nil
    {:class "my-class"}
    #j {:className "my-class"}
    {:class '(identity "my-class")}
    #j {:className (sablono.util/join-classes (identity "my-class"))}
    {:class "my-class"
     :style {:background-color "black"}}
    #j {:className "my-class"
        :style #j {:backgroundColor "black"}}
    {:class '(identity "my-class")
     :style {:background-color '(identity "black")}}
    #j {:className (sablono.util/join-classes (identity "my-class"))
        :style #j {:backgroundColor (identity "black")}}
    {:id :XY}
    #j {:id "XY"}))

(deftest test-attrs
  (are [form expected] (=== (attrs form) expected)
    nil nil
    {:class "my-class"}
    #j {:className "my-class"}
    {:class ["a" "b"]}
    #j {:className "a b"}
    {:class '(identity "my-class")}
    #j {:className (sablono.util/join-classes '(identity "my-class"))}
    {:class "my-class"
     :style {:background-color "black"}}
    #j {:className "my-class"
        :style #j {:backgroundColor "black"}}
    {:class '(identity "my-class")
     :style {:background-color '(identity "black")}}
    #j {:className (sablono.util/join-classes '(identity "my-class"))
        :style #j {:backgroundColor '(identity "black")}}))

(deftest test-to-js
  (let [v (to-js [])]
    (is (js-value? v))
    (is (= [] (.val v))))
  (let [v (to-js {})]
    (is (js-value? v))
    (is (= {} (.val v))))
  (let [v (to-js [1 [2] {:a 1 :b {:c [2 [3]]}}])]
    (is (js-value? v))
    (is (= 1 (first (.val v))))
    (is (= [2] (.val (second (.val v)))))
    (let [v (nth (.val v) 2)]
      (is (js-value? v))
      (is (= 1 (:a (.val v))))
      (let [v (:b (.val v))]
        (is (js-value? v))
        (let [v (:c (.val v))]
          (is (js-value? v))
          (is (= 2 (first (.val v))))
          (is (= [3] (.val (second (.val v))))))))))

(defspec test-basic-tags
  (prop/for-all
   [tag (s/gen keyword?)]
   (=== (eval `(compile [~tag]))
        `(react/createElement ~(name tag) nil))))

(deftest tag-names
  (testing "basic tags"
    (are-html
     '[:div] '(react/createElement "div" nil)
     '["div"] '(react/createElement "div" nil)
     '['div] '(react/createElement "div" nil)))
  (testing "tag syntax sugar"
    (are-html
     '[:div#foo] '(react/createElement "div" #j {:id "foo"})
     '[:div.foo] '(react/createElement "div" #j {:className "foo"})
     '[:div.foo (str "bar" "baz")]
     '(let* [attrs (str "bar" "baz")]
        (clojure.core/apply
         react/createElement "div"
         (if (clojure.core/map? attrs)
           (sablono.interpreter/attributes
            (sablono.normalize/merge-with-class {:class ["foo"]} attrs))
           #j {:className "foo"})
         (if (clojure.core/map? attrs)
           nil [(sablono.interpreter/interpret attrs)])))
     '[:div.a.b] '(react/createElement "div" #j {:className "a b"})
     '[:div.a.b.c] '(react/createElement "div" #j {:className "a b c"})
     '[:div#foo.bar.baz] '(react/createElement "div" #j {:id "foo", :className "bar baz"})
     '[:div.jumbotron] '(react/createElement "div" #j {:className "jumbotron"}))))

(deftest tag-contents
  (testing "empty tags"
    (are-html
     '[:div] '(react/createElement "div" nil)
     '[:h1] '(react/createElement "h1" nil)
     '[:script] '(react/createElement "script" nil)
     '[:text] '(react/createElement "text" nil)
     '[:a] '(react/createElement "a" nil)
     '[:iframe] '(react/createElement "iframe" nil)
     '[:title] '(react/createElement "title" nil)
     '[:section] '(react/createElement "section" nil)))
  (testing "tags containing text"
    (are-html
     '[:text "Lorem Ipsum"] '(react/createElement "text" nil "Lorem Ipsum")))
  (testing "contents are concatenated"
    (are-html
     '[:div "foo" "bar"]
     '(react/createElement "div" nil "foo" "bar")
     '[:div [:p] [:br]]
     '(react/createElement
       "div" nil
       (react/createElement "p" nil)
       (react/createElement "br" nil))))
  (testing "seqs are expanded"
    (are-html
     '[:div (list "foo" "bar")]
     '(let* [attrs (list "foo" "bar")]
        (clojure.core/apply
         react/createElement "div"
         (if (clojure.core/map? attrs)
           (sablono.interpreter/attributes attrs)
           nil)
         (if (clojure.core/map? attrs)
           nil [(sablono.interpreter/interpret attrs)])))
     '(list [:p "a"] [:p "b"])
     '(sablono.interpreter/interpret (list [:p "a"] [:p "b"]))))
  (testing "tags can contain tags"
    (are-html
     '[:div [:p]]
     '(react/createElement
       "div" nil
       (react/createElement "p" nil))
     '[:div [:b]]
     '(react/createElement
       "div" nil
       (react/createElement "b" nil))
     '[:p [:span [:a "foo"]]]
     '(react/createElement
       "p" nil
       (react/createElement
        "span" nil
        (react/createElement "a" nil "foo"))))))

(deftest tag-attributes
  (testing "tag with empty attribute map"
    (are-html
     '[:div {}] '(react/createElement "div" nil)))
  (testing "tag with populated attribute map"
    (are-html
     '[:div {:min "1", :max "2"}] '(react/createElement "div" #j {:min "1", :max "2"})
     '[:img {"id" "foo"}] '(react/createElement "img" #j {"id" "foo"})
     '[:img {:id "foo"}] '(react/createElement "img" #j {:id "foo"})))
  (testing "attribute values are escaped"
    (are-html
     '[:div {:id "\""}] '(react/createElement "div" #j {:id "\""})))
  (testing "attributes are converted to their DOM equivalents"
    (are-html
     '[:div {:class "classy"}] '(react/createElement "div" #j {:className "classy"})
     '[:div {:data-foo-bar "baz"}] '(react/createElement "div" #j {:data-foo-bar "baz"})
     '[:label {:for "foo"}] '(react/createElement "label" #j {:htmlFor "foo"})))
  (testing "boolean attributes"
    (are-html
     '[:input {:type "checkbox" :checked true}]
     '(sablono.interpreter/create-element "input" #j {:checked true, :type "checkbox"})
     '[:input {:type "checkbox" :checked false}]
     '(sablono.interpreter/create-element "input" #j {:checked false, :type "checkbox"})))
  (testing "nil attributes"
    (are-html
     '[:span {:class nil} "foo"] '(react/createElement "span" #j {:className nil} "foo")))
  (testing "empty attributes"
    (are-html
     '[:span {} "foo"] '(react/createElement "span" nil "foo")))
  (testing "tag with aria attributes"
    (are-html
     [:div {:aria-disabled true}]
     '(react/createElement "div" #j {:aria-disabled true})))
  (testing "tag with data attributes"
    (are-html
     [:div {:data-toggle "modal" :data-target "#modal"}]
     '(react/createElement "div" #j {:data-toggle "modal", :data-target "#modal"}))))

(deftest compiled-tags
  (testing "tag content can be vars, and vars can be type-hinted with some metadata"
    (let [x "foo"
          y {:id "id"}]
      (are-html
       '[:span x]
       '(let* [attrs x]
          (clojure.core/apply
           react/createElement "span"
           (if (clojure.core/map? attrs)
             (sablono.interpreter/attributes attrs)
             nil)
           (if (clojure.core/map? attrs)
             nil [(sablono.interpreter/interpret attrs)])))
       '[:span ^:attrs y]
       '(let* [attrs y]
          (clojure.core/apply
           react/createElement "span"
           (sablono.interpreter/attributes attrs) nil)))))
  (testing "tag content can be forms, and forms can be type-hinted with some metadata"
    (are-html
     '[:span (str (+ 1 1))]
     '(let* [attrs (str (+ 1 1))]
        (clojure.core/apply
         react/createElement "span"
         (if (clojure.core/map? attrs)
           (sablono.interpreter/attributes attrs)
           nil)
         (if (clojure.core/map? attrs)
           nil [(sablono.interpreter/interpret attrs)])))
     [:span ({:foo "bar"} :foo)] '(react/createElement "span" nil "bar")
     '[:span ^:attrs (merge {:type "button"} attrs)]
     '(let* [attrs (merge {:type "button"} attrs)]
        (clojure.core/apply
         react/createElement "span"
         (sablono.interpreter/attributes attrs) nil))))
  (testing "attributes can contain vars"
    (let [id "id"]
      (are-html
       '[:div {:id id}] '(react/createElement "div" #j {:id id})
       '[:div {:id id} "bar"] '(react/createElement "div" #j {:id id} "bar"))))
  (testing "attributes are evaluated"
    (are-html
     '[:img {:src (str "/foo" "/bar")}]
     '(react/createElement "img" #j {:src (str "/foo" "/bar")})
     '[:div {:id (str "a" "b")} (str "foo")]
     '(react/createElement "div" #j {:id (str "a" "b")} (sablono.interpreter/interpret (str "foo")))))
  (testing "type hints"
    (let [string "x"]
      (are-html
       '[:span ^String string] '(react/createElement "span" nil string))))
  (testing "values are evaluated only once"
    (let [times-called (atom 0)
          foo #(swap! times-called inc)]
      (html-expand [:div (foo)])
      (is (= @times-called 1)))))

(deftest test-benchmark-template
  (are-html
   '[:li
     [:a {:href (str "#show/" (:key datum))}]
     [:div {:id (str "item" (:key datum))
            :class ["class1" "class2"]}
      [:span {:class "anchor"} (:name datum)]]]
   '(react/createElement
     "li" nil
     (react/createElement
      "a" #j {:href (str "#show/" (:key datum))})
     (react/createElement
      "div" #j {:id (str "item" (:key datum)), :className "class1 class2"}
      (react/createElement
       "span" #j {:className "anchor"}
       (sablono.interpreter/interpret (:name datum)))))))

(deftest test-issue-2-merge-class
  (are-html
   '[:div.a {:class (if (true? true) "true" "false")}]
   '(react/createElement
     "div" #j {:className (sablono.util/join-classes ["a" (if (true? true) "true" "false")])})
   '[:div.a.b {:class (if (true? true) ["true"] "false")}]
   '(react/createElement
     "div" #j {:className (sablono.util/join-classes ["a" "b" (if (true? true) ["true"] "false")])})))

(deftest test-issue-3-recursive-js-literal
  (are-html
   '[:div.interaction-row {:style {:position "relative"}}]
   '(react/createElement "div" #j {:className "interaction-row", :style #j {:position "relative"}}))
  (let [username "foo", hidden #(if %1 {:display "none"} {:display "block"})]
    (are-html
     '[:ul.nav.navbar-nav.navbar-right.pull-right
       [:li.dropdown {:style (hidden (nil? username))}
        [:a.dropdown-toggle {:role "button" :href "#"} (str "Welcome, " username)
         [:span.caret]]
        [:ul.dropdown-menu {:role "menu" :style {:left 0}}]]]
     '(react/createElement
       "ul" #j {:className "nav navbar-nav navbar-right pull-right"}
       (react/createElement
        "li" #j {:style (sablono.interpreter/attributes
                         (hidden (nil? username)))
                 :className "dropdown"}
        (react/createElement
         "a" #j {:href "#", :role "button", :className "dropdown-toggle"}
         (sablono.interpreter/interpret (str "Welcome, " username))
         (react/createElement
          "span" #j {:className "caret"}))
        (react/createElement
         "ul" #j {:role "menu", :style #j {:left 0}, :className "dropdown-menu"}))))))

(deftest test-issue-22-id-after-class
  (are-html
   [:div.well#setup]
   '(react/createElement "div" #j {:id "setup", :className "well"})))

(deftest test-issue-25-comma-separated-class
  (are-html
   '[:div.c1.c2 "text"]
   '(react/createElement "div" #j {:className "c1 c2"} "text")
   '[:div.aa (merge {:class "bb"})]
   '(let* [attrs (merge {:class "bb"})]
      (clojure.core/apply
       react/createElement "div"
       (if (clojure.core/map? attrs)
         (sablono.interpreter/attributes
          (sablono.normalize/merge-with-class {:class ["aa"]} attrs))
         #j {:className "aa"})
       (if (clojure.core/map? attrs)
         nil [(sablono.interpreter/interpret attrs)])))))

(deftest test-issue-33-number-warning
  (are-html
   '[:div (count [1 2 3])]
   '(let* [attrs (count [1 2 3])]
      (clojure.core/apply
       react/createElement "div"
       (if (clojure.core/map? attrs)
         (sablono.interpreter/attributes attrs)
         nil)
       (if (clojure.core/map? attrs)
         nil [(sablono.interpreter/interpret attrs)])))))

(deftest test-issue-37-camel-case-style-attrs
  (are-html
   '[:div {:style {:z-index 1000}}]
   '(react/createElement "div" #j {:style #j {:zIndex 1000}})))

(deftest shorthand-div-forms
  (are-html
   [:#test]
   '(react/createElement "div" #j {:id "test"})
   '[:.klass]
   '(react/createElement "div" #j {:className "klass"})
   '[:#test.klass]
   '(react/createElement "div" #j {:id "test" :className "klass"})
   '[:#test.klass1.klass2]
   '(react/createElement "div" #j {:id "test" :className "klass1 klass2"})
   '[:.klass1.klass2#test]
   '(react/createElement "div" #j {:id "test" :className "klass1 klass2"})))

(deftest test-namespaced-fn-call
  (are-html
   '(some-ns/comp "arg")
   '(sablono.interpreter/interpret (some-ns/comp "arg"))
   '(some.ns/comp "arg")
   '(sablono.interpreter/interpret (some.ns/comp "arg"))))

(deftest test-compile-div-with-nested-lazy-seq
  (is (=== (compile [:div (map identity ["A" "B"])])
           '(let* [attrs (map identity ["A" "B"])]
              (clojure.core/apply
               react/createElement "div"
               (if (clojure.core/map? attrs)
                 (sablono.interpreter/attributes attrs)
                 nil)
               (if (clojure.core/map? attrs)
                 nil
                 [(sablono.interpreter/interpret attrs)]))))))

(deftest test-compile-div-with-nested-list
  (is (=== (compile [:div '("A" "B")])
           '(react/createElement "div" nil "A" "B"))))

(deftest test-compile-div-with-nested-vector
  (is (=== (compile [:div ["A" "B"]])
           '(react/createElement "div" nil "A" "B")))
  (is (=== (compile [:div (vector "A" "B")])
           '(let* [attrs (vector "A" "B")]
              (clojure.core/apply
               react/createElement "div"
               (if (clojure.core/map? attrs)
                 (sablono.interpreter/attributes attrs)
                 nil)
               (if (clojure.core/map? attrs)
                 nil
                 [(sablono.interpreter/interpret attrs)]))))))

(deftest test-class-as-set
  (is (=== (compile [:div.a {:class #{"a" "b" "c"}}])
           '(react/createElement "div" #j {:className "a a b c"}))))

(deftest test-class-as-list
  (is (=== (compile [:div.a {:class (list "a" "b" "c")}])
           '(react/createElement "div" #j {:className (sablono.util/join-classes ["a" (list "a" "b" "c")])}))))

(deftest test-class-as-vector
  (is (=== (compile [:div.a {:class (vector "a" "b" "c")}])
           '(react/createElement
             "div" #j {:className (sablono.util/join-classes ["a" (vector "a" "b" "c")])}))))

(deftest test-class-merge-symbol
  (let [class #{"b"}]
    (are-html
     [:div.a {:class class}]
     '(react/createElement "div" #j {:className "a b"}))))

(deftest test-issue-90
  (is (=== (compile [:div nil (case :a :a "a")])
           '(react/createElement
             "div" nil nil
             (sablono.interpreter/interpret
              (case :a :a "a"))))))

(deftest test-compile-attr-class
  (are [form expected]
      (=== expected (compile-attr :class form))
    nil nil
    "foo" "foo"
    '("foo" "bar" ) "foo bar"
    ["foo" "bar"] "foo bar"
    #{"foo" "bar"} "foo bar"
    '(set "foo" "bar")
    '(sablono.util/join-classes (set "foo" "bar"))
    '[(list "foo" "bar")]
    '(sablono.util/join-classes [(list "foo" "bar")])))

(deftest test-optimize-let-form
  (is (=== (compile (let [x "x"] [:div "x"]))
           '(let* [x "x"] (react/createElement "div" nil "x")))))

(deftest test-optimize-for-loop
  (is (=== (compile [:ul (for [n (range 3)] [:li n])])
           '(react/createElement
             "ul" nil
             (into-array
              (clojure.core/for [n (range 3)]
                (clojure.core/let [attrs n]
                  (clojure.core/apply
                   react/createElement "li"
                   (if (clojure.core/map? attrs)
                     (sablono.interpreter/attributes attrs)
                     nil)
                   (if (clojure.core/map? attrs)
                     nil [(sablono.interpreter/interpret attrs)]))))))))
  (is (=== (compile [:ul (for [n (range 3)] [:li ^:attrs n])])
           '(react/createElement
             "ul" nil
             (into-array
              (clojure.core/for [n (range 3)]
                (clojure.core/let [attrs n]
                  (clojure.core/apply
                   react/createElement "li"
                   (sablono.interpreter/attributes attrs) nil))))))))

(deftest test-optimize-if
  (is (=== (compile (if true [:span "foo"] [:span "bar"]) )
           '(if true
              (react/createElement "span" nil "foo")
              (react/createElement "span" nil "bar")))))

(deftest test-issue-115
  (is (=== (compile [:a {:id :XY}])
           '(react/createElement "a" #j {:id "XY"}))))

(deftest test-issue-130
  (let [css {:table-cell "bg-blue"}]
    (is (=== (compile [:div {:class (:table-cell css)} [:span "abc"]])
             '(react/createElement
               "div"
               #j {:className (sablono.util/join-classes [(:table-cell css)])}
               (react/createElement "span" nil "abc"))))))

(deftest test-issue-141-inline
  (testing "with attributes"
    (is (=== (compile [:span {} ^:inline (constantly 1)])
             '(react/createElement "span" nil (constantly 1)))))
  (testing "without attributes"
    (is (=== (compile [:span ^:inline (constantly 1)])
             '(react/createElement "span" nil (constantly 1))))))

(deftest test-compile-attributes-non-literal-key
  (is (=== (compile [:input {(case :checkbox :checkbox :checked :value) "x"}])
           '(sablono.interpreter/create-element
             "input" (sablono.interpreter/attributes
                      {(case :checkbox :checkbox :checked :value) "x"})))))

(deftest test-issue-158
  (is (=== (compile [:div {:style (merge {:margin-left "2rem"}
                                         (when focused? {:color "red"}))}])
           '(react/createElement
             "div" #j {:style (sablono.interpreter/attributes
                               (merge {:margin-left "2rem"}
                                      (when focused? {:color "red"})))}))))
