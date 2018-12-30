(ns sablono.compiler-test
  (:refer-clojure :exclude [compile])
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
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

(def gen-tag
  (gen/such-that (complement fragment?) (s/gen keyword?)))

(defspec test-basic-tags
  (prop/for-all
    [tag gen-tag]
    (=== (eval `(compile [~tag]))
         `(sablono.core/create-element ~(name tag) nil))))

(deftest tag-names
  (testing "basic tags"
    (are-html
     '[:div] '(sablono.core/create-element "div" nil)
     '["div"] '(sablono.core/create-element "div" nil)
     '['div] '(sablono.core/create-element "div" nil)))
  (testing "tag syntax sugar"
    (are-html
     '[:div#foo] '(sablono.core/create-element "div" #j {:id "foo"})
     '[:div.foo] '(sablono.core/create-element "div" #j {:className "foo"})
     '[:div.foo (str "bar" "baz")]
     '(let* [attrs (str "bar" "baz")]
        (clojure.core/apply
         sablono.core/create-element "div"
         (if (clojure.core/map? attrs)
           (sablono.interpreter/attributes
            (sablono.normalize/merge-with-class {:class ["foo"]} attrs))
           #j {:className "foo"})
         (if (clojure.core/map? attrs)
           nil [(sablono.interpreter/interpret attrs)])))
     '[:div.a.b] '(sablono.core/create-element "div" #j {:className "a b"})
     '[:div.a.b.c] '(sablono.core/create-element "div" #j {:className "a b c"})
     '[:div#foo.bar.baz] '(sablono.core/create-element "div" #j {:id "foo", :className "bar baz"})
     '[:div.jumbotron] '(sablono.core/create-element "div" #j {:className "jumbotron"}))))

(deftest tag-contents
  (testing "empty tags"
    (are-html
     '[:div] '(sablono.core/create-element "div" nil)
     '[:h1] '(sablono.core/create-element "h1" nil)
     '[:script] '(sablono.core/create-element "script" nil)
     '[:text] '(sablono.core/create-element "text" nil)
     '[:a] '(sablono.core/create-element "a" nil)
     '[:iframe] '(sablono.core/create-element "iframe" nil)
     '[:title] '(sablono.core/create-element "title" nil)
     '[:section] '(sablono.core/create-element "section" nil)))
  (testing "tags containing text"
    (are-html
     '[:text "Lorem Ipsum"] '(sablono.core/create-element "text" nil "Lorem Ipsum")))
  (testing "contents are concatenated"
    (are-html
     '[:div "foo" "bar"]
     '(sablono.core/create-element "div" nil "foo" "bar")
     '[:div [:p] [:br]]
     '(sablono.core/create-element
       "div" nil
       (sablono.core/create-element "p" nil)
       (sablono.core/create-element "br" nil))))
  (testing "seqs are expanded"
    (are-html
     '[:div (list "foo" "bar")]
     '(let* [attrs (list "foo" "bar")]
        (clojure.core/apply
         sablono.core/create-element "div"
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
     '(sablono.core/create-element
       "div" nil
       (sablono.core/create-element "p" nil))
     '[:div [:b]]
     '(sablono.core/create-element
       "div" nil
       (sablono.core/create-element "b" nil))
     '[:p [:span [:a "foo"]]]
     '(sablono.core/create-element
       "p" nil
       (sablono.core/create-element
        "span" nil
        (sablono.core/create-element "a" nil "foo"))))))

(deftest tag-attributes
  (testing "tag with empty attribute map"
    (are-html
     '[:div {}] '(sablono.core/create-element "div" nil)))
  (testing "tag with populated attribute map"
    (are-html
     '[:div {:min "1", :max "2"}] '(sablono.core/create-element "div" #j {:min "1", :max "2"})
     '[:img {"id" "foo"}] '(sablono.core/create-element "img" #j {"id" "foo"})
     '[:img {:id "foo"}] '(sablono.core/create-element "img" #j {:id "foo"})))
  (testing "attribute values are escaped"
    (are-html
     '[:div {:id "\""}] '(sablono.core/create-element "div" #j {:id "\""})))
  (testing "attributes are converted to their DOM equivalents"
    (are-html
     '[:div {:class "classy"}] '(sablono.core/create-element "div" #j {:className "classy"})
     '[:div {:data-foo-bar "baz"}] '(sablono.core/create-element "div" #j {:data-foo-bar "baz"})
     '[:label {:for "foo"}] '(sablono.core/create-element "label" #j {:htmlFor "foo"})))
  (testing "boolean attributes"
    (are-html
     '[:input {:type "checkbox" :checked true}]
     '(sablono.interpreter/create-element "input" #j {:checked true, :type "checkbox"})
     '[:input {:type "checkbox" :checked false}]
     '(sablono.interpreter/create-element "input" #j {:checked false, :type "checkbox"})))
  (testing "nil attributes"
    (are-html
     '[:span {:class nil} "foo"] '(sablono.core/create-element "span" #j {:className nil} "foo")))
  (testing "empty attributes"
    (are-html
     '[:span {} "foo"] '(sablono.core/create-element "span" nil "foo")))
  (testing "tag with aria attributes"
    (are-html
     [:div {:aria-disabled true}]
     '(sablono.core/create-element "div" #j {:aria-disabled true})))
  (testing "tag with data attributes"
    (are-html
     [:div {:data-toggle "modal" :data-target "#modal"}]
     '(sablono.core/create-element "div" #j {:data-toggle "modal", :data-target "#modal"}))))

(deftest compiled-tags
  (testing "tag content can be vars, and vars can be type-hinted with some metadata"
    (let [x "foo"
          y {:id "id"}]
      (are-html
       '[:span x]
       '(let* [attrs x]
          (clojure.core/apply
           sablono.core/create-element "span"
           (if (clojure.core/map? attrs)
             (sablono.interpreter/attributes attrs)
             nil)
           (if (clojure.core/map? attrs)
             nil [(sablono.interpreter/interpret attrs)])))
       '[:span ^:attrs y]
       '(let* [attrs y]
          (clojure.core/apply
           sablono.core/create-element "span"
           (sablono.interpreter/attributes attrs) nil)))))
  (testing "tag content can be forms, and forms can be type-hinted with some metadata"
    (are-html
     '[:span (str (+ 1 1))]
     '(let* [attrs (str (+ 1 1))]
        (clojure.core/apply
         sablono.core/create-element "span"
         (if (clojure.core/map? attrs)
           (sablono.interpreter/attributes attrs)
           nil)
         (if (clojure.core/map? attrs)
           nil [(sablono.interpreter/interpret attrs)])))
     [:span ({:foo "bar"} :foo)] '(sablono.core/create-element "span" nil "bar")
     '[:span ^:attrs (merge {:type "button"} attrs)]
     '(let* [attrs (merge {:type "button"} attrs)]
        (clojure.core/apply
         sablono.core/create-element "span"
         (sablono.interpreter/attributes attrs) nil))))
  (testing "attributes can contain vars"
    (let [id "id"]
      (are-html
       '[:div {:id id}] '(sablono.core/create-element "div" #j {:id id})
       '[:div {:id id} "bar"] '(sablono.core/create-element "div" #j {:id id} "bar"))))
  (testing "attributes are evaluated"
    (are-html
     '[:img {:src (str "/foo" "/bar")}]
     '(sablono.core/create-element "img" #j {:src (str "/foo" "/bar")})
     '[:div {:id (str "a" "b")} (str "foo")]
     '(sablono.core/create-element "div" #j {:id (str "a" "b")} (sablono.interpreter/interpret (str "foo")))))
  (testing "type hints"
    (let [string "x"]
      (are-html
       '[:span ^String string] '(sablono.core/create-element "span" nil string))))
  (testing "values are evaluated only once"
    (let [times-called (atom 0)
          foo #(swap! times-called inc)]
      (html-expand [:div (foo)])
      (is (= @times-called 1)))))

(deftest fragments
  (testing "React 16 fragment syntactic support"
    (are-html
     '[:*]
     '(sablono.core/create-element
       sablono.core/fragment nil)

     '[:* [:p]]
     '(sablono.core/create-element
       sablono.core/fragment nil
       (sablono.core/create-element "p" nil))

     '[:* [:p] [:p]]
     '(sablono.core/create-element
       sablono.core/fragment nil
       (sablono.core/create-element "p" nil)
       (sablono.core/create-element "p" nil))

     '[:dl (for [n (range 2)]
             [:* {:key n}
              [:dt {} (str "term " n)]
              [:dd {} (str "definition " n)]])]
     '(sablono.core/create-element
       "dl" nil
       (into-array
        (clojure.core/for
            [n (range 2)]
          (sablono.core/create-element
           sablono.core/fragment #j {:key n}
           (sablono.core/create-element "dt" nil (sablono.interpreter/interpret (str "term " n)))
           (sablono.core/create-element "dd" nil (sablono.interpreter/interpret (str "definition " n))))))))))

(deftest test-benchmark-template
  (are-html
   '[:li
     [:a {:href (str "#show/" (:key datum))}]
     [:div {:id (str "item" (:key datum))
            :class ["class1" "class2"]}
      [:span {:class "anchor"} (:name datum)]]]
   '(sablono.core/create-element
     "li" nil
     (sablono.core/create-element
      "a" #j {:href (str "#show/" (:key datum))})
     (sablono.core/create-element
      "div" #j {:id (str "item" (:key datum)), :className "class1 class2"}
      (sablono.core/create-element
       "span" #j {:className "anchor"}
       (sablono.interpreter/interpret (:name datum)))))))

(deftest test-issue-2-merge-class
  (are-html
   '[:div.a {:class (if (true? true) "true" "false")}]
   '(sablono.core/create-element
     "div" #j {:className (sablono.util/join-classes ["a" (if (true? true) "true" "false")])})
   '[:div.a.b {:class (if (true? true) ["true"] "false")}]
   '(sablono.core/create-element
     "div" #j {:className (sablono.util/join-classes ["a" "b" (if (true? true) ["true"] "false")])})))

(deftest test-issue-3-recursive-js-literal
  (are-html
   '[:div.interaction-row {:style {:position "relative"}}]
   '(sablono.core/create-element "div" #j {:className "interaction-row", :style #j {:position "relative"}}))
  (let [username "foo", hidden #(if %1 {:display "none"} {:display "block"})]
    (are-html
     '[:ul.nav.navbar-nav.navbar-right.pull-right
       [:li.dropdown {:style (hidden (nil? username))}
        [:a.dropdown-toggle {:role "button" :href "#"} (str "Welcome, " username)
         [:span.caret]]
        [:ul.dropdown-menu {:role "menu" :style {:left 0}}]]]
     '(sablono.core/create-element
       "ul" #j {:className "nav navbar-nav navbar-right pull-right"}
       (sablono.core/create-element
        "li" #j {:style (sablono.interpreter/attributes
                         (hidden (nil? username)))
                 :className "dropdown"}
        (sablono.core/create-element
         "a" #j {:href "#", :role "button", :className "dropdown-toggle"}
         (sablono.interpreter/interpret (str "Welcome, " username))
         (sablono.core/create-element
          "span" #j {:className "caret"}))
        (sablono.core/create-element
         "ul" #j {:role "menu", :style #j {:left 0}, :className "dropdown-menu"}))))))

(deftest test-issue-22-id-after-class
  (are-html
   [:div.well#setup]
   '(sablono.core/create-element "div" #j {:id "setup", :className "well"})))

(deftest test-issue-25-comma-separated-class
  (are-html
   '[:div.c1.c2 "text"]
   '(sablono.core/create-element "div" #j {:className "c1 c2"} "text")
   '[:div.aa (merge {:class "bb"})]
   '(let* [attrs (merge {:class "bb"})]
      (clojure.core/apply
       sablono.core/create-element "div"
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
       sablono.core/create-element "div"
       (if (clojure.core/map? attrs)
         (sablono.interpreter/attributes attrs)
         nil)
       (if (clojure.core/map? attrs)
         nil [(sablono.interpreter/interpret attrs)])))))

(deftest test-issue-37-camel-case-style-attrs
  (are-html
   '[:div {:style {:z-index 1000}}]
   '(sablono.core/create-element "div" #j {:style #j {:zIndex 1000}})))

(deftest shorthand-div-forms
  (are-html
   [:#test]
   '(sablono.core/create-element "div" #j {:id "test"})
   '[:.klass]
   '(sablono.core/create-element "div" #j {:className "klass"})
   '[:#test.klass]
   '(sablono.core/create-element "div" #j {:id "test" :className "klass"})
   '[:#test.klass1.klass2]
   '(sablono.core/create-element "div" #j {:id "test" :className "klass1 klass2"})
   '[:.klass1.klass2#test]
   '(sablono.core/create-element "div" #j {:id "test" :className "klass1 klass2"})))

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
               sablono.core/create-element "div"
               (if (clojure.core/map? attrs)
                 (sablono.interpreter/attributes attrs)
                 nil)
               (if (clojure.core/map? attrs)
                 nil
                 [(sablono.interpreter/interpret attrs)]))))))

(deftest test-compile-div-with-nested-list
  (is (=== (compile [:div '("A" "B")])
           '(sablono.core/create-element "div" nil "A" "B"))))

(deftest test-compile-div-with-nested-vector
  (is (=== (compile [:div ["A" "B"]])
           '(sablono.core/create-element "div" nil "A" "B")))
  (is (=== (compile [:div (vector "A" "B")])
           '(let* [attrs (vector "A" "B")]
              (clojure.core/apply
               sablono.core/create-element "div"
               (if (clojure.core/map? attrs)
                 (sablono.interpreter/attributes attrs)
                 nil)
               (if (clojure.core/map? attrs)
                 nil
                 [(sablono.interpreter/interpret attrs)]))))))

(deftest test-class-as-set
  (is (=== (compile [:div.a {:class #{"a" "b" "c"}}])
           '(sablono.core/create-element "div" #j {:className "a a b c"}))))

(deftest test-class-as-list
  (is (=== (compile [:div.a {:class (list "a" "b" "c")}])
           '(sablono.core/create-element "div" #j {:className (sablono.util/join-classes ["a" (list "a" "b" "c")])}))))

(deftest test-class-as-vector
  (is (=== (compile [:div.a {:class (vector "a" "b" "c")}])
           '(sablono.core/create-element
             "div" #j {:className (sablono.util/join-classes ["a" (vector "a" "b" "c")])}))))

(deftest test-class-merge-symbol
  (let [class #{"b"}]
    (are-html
     [:div.a {:class class}]
     '(sablono.core/create-element "div" #j {:className "a b"}))))

(deftest test-issue-90
  (is (=== (compile [:div nil (case :a :a "a")])
           '(sablono.core/create-element
             "div" nil nil (clojure.core/case :a :a "a")))))

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
           '(let* [x "x"] (sablono.core/create-element "div" nil "x")))))

(deftest test-optimize-for-loop
  (is (=== (compile [:ul (for [n (range 3)] [:li n])])
           '(sablono.core/create-element
             "ul" nil
             (into-array
              (clojure.core/for [n (range 3)]
                (clojure.core/let [attrs n]
                  (clojure.core/apply
                   sablono.core/create-element "li"
                   (if (clojure.core/map? attrs)
                     (sablono.interpreter/attributes attrs)
                     nil)
                   (if (clojure.core/map? attrs)
                     nil [(sablono.interpreter/interpret attrs)]))))))))
  (is (=== (compile [:ul (for [n (range 3)] [:li ^:attrs n])])
           '(sablono.core/create-element
             "ul" nil
             (into-array
              (clojure.core/for [n (range 3)]
                (clojure.core/let [attrs n]
                  (clojure.core/apply
                   sablono.core/create-element "li"
                   (sablono.interpreter/attributes attrs) nil))))))))

(deftest test-compile-case
  (is (=== (compile [:div {:class "a"}
                     (case "a"
                       "a" [:div "a"]
                       "b" [:div "b"]
                       [:div "else"])])
           '(sablono.core/create-element
             "div" #j {:className "a"}
             (clojure.core/case "a"
               "a" (sablono.core/create-element "div" nil "a")
               "b" (sablono.core/create-element "div" nil "b")
               (sablono.core/create-element "div" nil "else"))))))

(deftest test-compile-cond
  (is (=== (compile [:div {:class "a"}
                     (condp = "a"
                       "a" [:div "a"]
                       "b" [:div "b"]
                       [:div "else"])])
           '(sablono.core/create-element
             "div" #j {:className "a"}
             (clojure.core/condp = "a"
               "a" (sablono.core/create-element "div" nil "a")
               "b" (sablono.core/create-element "div" nil "b")
               (sablono.core/create-element "div" nil "else"))))))

(deftest test-compile-condp
  (is (=== (compile [:div {:class "a"}
                     (cond
                       (= "a" "a") [:div "a"]
                       (= "b" "b") [:div "b"]
                       :else [:div "else"])])
           '(sablono.core/create-element
             "div" #j {:className "a"}
             (clojure.core/cond
               (= "a" "a")
               (sablono.core/create-element "div" nil "a")
               (= "b" "b")
               (sablono.core/create-element "div" nil "b")
               :else
               (sablono.core/create-element "div" nil "else"))))))

(deftest test-optimize-if
  (is (=== (compile (if true [:span "foo"] [:span "bar"]) )
           '(if true
              (sablono.core/create-element "span" nil "foo")
              (sablono.core/create-element "span" nil "bar")))))

(deftest test-compile-if-not
  (is (=== (compile [:div {:class "a"} (if-not false [:div [:div]])])
           '(sablono.core/create-element
             "div" #j {:className "a"}
             (clojure.core/if-not false
               (sablono.core/create-element
                "div" nil (sablono.core/create-element "div" nil)))))))

(deftest test-compile-if-some
  (is (=== (compile [:div {:class "a"} (if-some [x true] [:div [:div]])])
           '(sablono.core/create-element
             "div" #j {:className "a"}
             (clojure.core/if-some [x true]
               (sablono.core/create-element
                "div" nil (sablono.core/create-element "div" nil)))))))

(deftest test-issue-115
  (is (=== (compile [:a {:id :XY}])
           '(sablono.core/create-element "a" #j {:id "XY"}))))

(deftest test-issue-130
  (let [css {:table-cell "bg-blue"}]
    (is (=== (compile [:div {:class (:table-cell css)} [:span "abc"]])
             '(sablono.core/create-element
               "div"
               #j {:className (sablono.util/join-classes [(:table-cell css)])}
               (sablono.core/create-element "span" nil "abc"))))))

(deftest test-issue-141-inline
  (testing "with attributes"
    (is (=== (compile [:span {} ^:inline (constantly 1)])
             '(sablono.core/create-element "span" nil (constantly 1)))))
  (testing "without attributes"
    (is (=== (compile [:span ^:inline (constantly 1)])
             '(sablono.core/create-element "span" nil (constantly 1))))))

(deftest test-compile-attributes-non-literal-key
  (is (=== (compile [:input {(case :checkbox :checkbox :checked :value) "x"}])
           '(sablono.interpreter/create-element
             "input" (sablono.interpreter/attributes
                      {(case :checkbox :checkbox :checked :value) "x"})))))

(deftest test-issue-158
  (is (=== (compile [:div {:style (merge {:margin-left "2rem"}
                                         (when focused? {:color "red"}))}])
           '(sablono.core/create-element
             "div" #j {:style (sablono.interpreter/attributes
                               (merge {:margin-left "2rem"}
                                      (when focused? {:color "red"})))}))))


(deftest test-compile-when
  (is (=== (compile [:div {:class "a"} (when true [:div [:div]])])
           '(sablono.core/create-element
             "div" #j {:className "a"}
             (clojure.core/when true
               (sablono.core/create-element
                "div" nil (sablono.core/create-element "div" nil)))))))

(deftest test-compile-when-not
  (is (=== (compile [:div {:class "a"} (when-not false [:div [:div]])])
           '(sablono.core/create-element
             "div" #j {:className "a"}
             (clojure.core/when-not false
               (sablono.core/create-element
                "div" nil (sablono.core/create-element "div" nil)))))))

(deftest test-compile-when-some
  (is (=== (compile [:div {:class "a"} (when-some [x true] [:div [:div]])])
           '(sablono.core/create-element
             "div" #j {:className "a"}
             (clojure.core/when-some [x true]
               (sablono.core/create-element
                "div" nil (sablono.core/create-element "div" nil)))))))
