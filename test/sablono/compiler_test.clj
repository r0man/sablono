(ns sablono.compiler-test
  (:refer-clojure :exclude [compile])
  (:require [cljs.tagged-literals :refer [valid-js-literal-key?]]
            [clojure.spec :as s]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.walk :refer [prewalk]]
            [sablono.compiler :refer :all]
            [sablono.core :refer [attrs html html-expand]]
            [sablono.interpreter :as interpreter])
  (:import cljs.tagged_literals.JSValue))

;; Because instances of cljs.tagged_literals.JSValue are not
;; comparable via = we define our own version of JSValue and use this
;; with the reader literal #j

(deftype JSValueWrapper [val]
  Object
  (equals [this other]
    (and (instance? JSValueWrapper other)
         (= (.val this) (.val other))))
  (hashCode [this]
    (.hashCode (.val this)))
  (toString [this]
    (.toString (.val this))))

(defmethod print-method JSValueWrapper
  [^JSValueWrapper v, ^java.io.Writer w]
  (.write w "#j ")
  (.write w (pr-str (.val v))))

(defn read-js
  [form]
  (when-not (or (vector? form)
                (map? form))
    (throw (ex-info "JavaScript literal must use map or vector notation"
                    {:form form})))
  (when-not (or (not (map? form))
                (every? valid-js-literal-key? (keys form)))
    (throw (ex-info (str "JavaScript literal keys must be strings "
                         "or unqualified keywords")
                    {:form form})))
  (JSValueWrapper. form))

(defn replace-js-value [forms]
  (prewalk
   (fn [form]
     (if (instance? JSValue form)
       (JSValueWrapper. (replace-js-value (.val form))) form))
   forms))

(defn replace-gensyms [forms]
  (prewalk
   (fn [form]
     (if (and (symbol? form)
              (re-matches #"attrs\d+" (str form)))
       'attrs form))
   forms))

(defmacro compile [form]
  `(replace-js-value (replace-gensyms (macroexpand '(html ~form)))))

(defmacro are-html-expanded [& body]
  `(are [form# expected#]
       (is (= (replace-js-value (replace-gensyms (html-expand form#)))
              expected#))
     ~@body))

(deftest test-compile-attrs
  (are [attrs expected] (= (replace-js-value (compile-attrs attrs)) expected)
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
  (are [form expected]
      (= (replace-js-value (attrs form)) expected)
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
    (is (instance? JSValue v))
    (is (= [] (.val v))))
  (let [v (to-js {})]
    (is (instance? JSValue v))
    (is (= {} (.val v))))
  (let [v (to-js [1 [2] {:a 1 :b {:c [2 [3]]}}])]
    (is (instance? JSValue v))
    (is (= 1 (first (.val v))))
    (is (= [2] (.val (second (.val v)))))
    (let [v (nth (.val v) 2)]
      (is (instance? JSValue v))
      (is (= 1 (:a (.val v))))
      (let [v (:b (.val v))]
        (is (instance? JSValue v))
        (let [v (:c (.val v))]
          (is (instance? JSValue v))
          (is (= 2 (first (.val v))))
          (is (= [3] (.val (second (.val v))))))))))

(defspec test-basic-tags
  (prop/for-all
   [tag (s/gen keyword?)]
   (= (eval `(compile [~tag]))
      `(js/React.createElement ~(name tag) nil))))

(deftest tag-names
  (testing "basic tags"
    (are-html-expanded
     '[:div] '(js/React.createElement "div" nil)
     '["div"] '(js/React.createElement "div" nil)
     '['div] '(js/React.createElement "div" nil)))
  (testing "tag syntax sugar"
    (are-html-expanded
     '[:div#foo] '(js/React.createElement "div" #j {:id "foo"})
     '[:div.foo] '(js/React.createElement "div" #j {:className "foo"})
     '[:div.foo (str "bar" "baz")]
     '(let* [attrs (str "bar" "baz")]
        (clojure.core/apply
         js/React.createElement "div"
         (if (clojure.core/map? attrs)
           (sablono.interpreter/attributes
            (sablono.normalize/merge-with-class {:class ["foo"]} attrs))
           #j {:className "foo"})
         (if (clojure.core/map? attrs)
           nil [(sablono.interpreter/interpret attrs)])))
     '[:div.a.b] '(js/React.createElement "div" #j {:className "a b"})
     '[:div.a.b.c] '(js/React.createElement "div" #j {:className "a b c"})
     '[:div#foo.bar.baz] '(js/React.createElement "div" #j {:id "foo", :className "bar baz"})
     '[:div.jumbotron] '(js/React.createElement "div" #j {:className "jumbotron"}))))

(deftest tag-contents
  (testing "empty tags"
    (are-html-expanded
     '[:div] '(js/React.createElement "div" nil)
     '[:h1] '(js/React.createElement "h1" nil)
     '[:script] '(js/React.createElement "script" nil)
     '[:text] '(js/React.createElement "text" nil)
     '[:a] '(js/React.createElement "a" nil)
     '[:iframe] '(js/React.createElement "iframe" nil)
     '[:title] '(js/React.createElement "title" nil)
     '[:section] '(js/React.createElement "section" nil)))
  (testing "tags containing text"
    (are-html-expanded
     '[:text "Lorem Ipsum"] '(js/React.createElement "text" nil "Lorem Ipsum")))
  (testing "contents are concatenated"
    (are-html-expanded
     '[:div "foo" "bar"]
     '(js/React.createElement "div" nil "foo" "bar")
     '[:div [:p] [:br]]
     '(js/React.createElement
       "div" nil
       (js/React.createElement "p" nil)
       (js/React.createElement "br" nil))))
  (testing "seqs are expanded"
    (are-html-expanded
     '[:div (list "foo" "bar")]
     '(let* [attrs (list "foo" "bar")]
        (clojure.core/apply
         js/React.createElement "div"
         (if (clojure.core/map? attrs)
           (sablono.interpreter/attributes attrs)
           nil)
         (if (clojure.core/map? attrs)
           nil [(sablono.interpreter/interpret attrs)])))
     '(list [:p "a"] [:p "b"])
     '(sablono.interpreter/interpret (list [:p "a"] [:p "b"]))))
  (testing "vecs don't expand - error if vec doesn't have tag name"
    (is (thrown? Exception (html (vector [:p "a"] [:p "b"])))))
  (testing "tags can contain tags"
    (are-html-expanded
     '[:div [:p]]
     '(js/React.createElement
       "div" nil
       (js/React.createElement "p" nil))
     '[:div [:b]]
     '(js/React.createElement
       "div" nil
       (js/React.createElement "b" nil))
     '[:p [:span [:a "foo"]]]
     '(js/React.createElement
       "p" nil
       (js/React.createElement
        "span" nil
        (js/React.createElement "a" nil "foo"))))))

(deftest tag-attributes
  (testing "tag with empty attribute map"
    (are-html-expanded
     '[:div {}] '(js/React.createElement "div" nil)))
  (testing "tag with populated attribute map"
    (are-html-expanded
     '[:div {:min "1", :max "2"}] '(js/React.createElement "div" #j {:min "1", :max "2"})
     '[:img {"id" "foo"}] '(js/React.createElement "img" #j {"id" "foo"})
     '[:img {:id "foo"}] '(js/React.createElement "img" #j {:id "foo"})))
  (testing "attribute values are escaped"
    (are-html-expanded
     '[:div {:id "\""}] '(js/React.createElement "div" #j {:id "\""})))
  (testing "attributes are converted to their DOM equivalents"
    (are-html-expanded
     '[:div {:class "classy"}] '(js/React.createElement "div" #j {:className "classy"})
     '[:div {:data-foo-bar "baz"}] '(js/React.createElement "div" #j {:data-foo-bar "baz"})
     '[:label {:for "foo"}] '(js/React.createElement "label" #j {:htmlFor "foo"})))
  (testing "boolean attributes"
    (are-html-expanded
     '[:input {:type "checkbox" :checked true}]
     '(sablono.interpreter/create-element "input" #j {:checked true, :type "checkbox"})
     '[:input {:type "checkbox" :checked false}]
     '(sablono.interpreter/create-element "input" #j {:checked false, :type "checkbox"})))
  (testing "nil attributes"
    (are-html-expanded
     '[:span {:class nil} "foo"] '(js/React.createElement "span" #j {:className nil} "foo")))
  (testing "empty attributes"
    (are-html-expanded
     '[:span {} "foo"] '(js/React.createElement "span" nil "foo")))
  (testing "tag with aria attributes"
    (are-html-expanded
     [:div {:aria-disabled true}]
     '(js/React.createElement "div" #j {:aria-disabled true})))
  (testing "tag with data attributes"
    (are-html-expanded
     [:div {:data-toggle "modal" :data-target "#modal"}]
     '(js/React.createElement "div" #j {:data-toggle "modal", :data-target "#modal"}))))

(deftest compiled-tags
  (testing "tag content can be vars, and vars can be type-hinted with some metadata"
    (let [x "foo"
          y {:id "id"}]
      (are-html-expanded
       '[:span x]
       '(let* [attrs x]
          (clojure.core/apply
           js/React.createElement "span"
           (if (clojure.core/map? attrs)
             (sablono.interpreter/attributes attrs)
             nil)
           (if (clojure.core/map? attrs)
             nil [(sablono.interpreter/interpret attrs)])))
       '[:span ^:attrs y]
       '(let* [attrs y]
          (clojure.core/apply
           js/React.createElement "span"
           (sablono.interpreter/attributes attrs) nil)))))
  (testing "tag content can be forms, and forms can be type-hinted with some metadata"
    (are-html-expanded
     '[:span (str (+ 1 1))]
     '(let* [attrs (str (+ 1 1))]
        (clojure.core/apply
         js/React.createElement "span"
         (if (clojure.core/map? attrs)
           (sablono.interpreter/attributes attrs)
           nil)
         (if (clojure.core/map? attrs)
           nil [(sablono.interpreter/interpret attrs)])))
     [:span ({:foo "bar"} :foo)] '(js/React.createElement "span" nil "bar")
     '[:span ^:attrs (merge {:type "button"} attrs)]
     '(let* [attrs (merge {:type "button"} attrs)]
        (clojure.core/apply
         js/React.createElement "span"
         (sablono.interpreter/attributes attrs) nil))))
  (testing "attributes can contain vars"
    (let [id "id"]
      (are-html-expanded
       '[:div {:id id}] '(js/React.createElement "div" #j {:id id})
       '[:div {:id id} "bar"] '(js/React.createElement "div" #j {:id id} "bar"))))
  (testing "attributes are evaluated"
    (are-html-expanded
     '[:img {:src (str "/foo" "/bar")}]
     '(js/React.createElement "img" #j {:src (str "/foo" "/bar")})
     '[:div {:id (str "a" "b")} (str "foo")]
     '(js/React.createElement "div" #j {:id (str "a" "b")} (sablono.interpreter/interpret (str "foo")))))
  (testing "type hints"
    (let [string "x"]
      (are-html-expanded
       '[:span ^String string] '(js/React.createElement "span" nil string))))
  (testing "values are evaluated only once"
    (let [times-called (atom 0)
          foo #(swap! times-called inc)]
      (html-expand [:div (foo)])
      (is (= @times-called 1)))))

(deftest test-benchmark-template
  (are-html-expanded
   '[:li
     [:a {:href (str "#show/" (:key datum))}]
     [:div {:id (str "item" (:key datum))
            :class ["class1" "class2"]}
      [:span {:class "anchor"} (:name datum)]]]
   '(js/React.createElement
     "li" nil
     (js/React.createElement
      "a" #j {:href (str "#show/" (:key datum))})
     (js/React.createElement
      "div" #j {:id (str "item" (:key datum)), :className "class1 class2"}
      (js/React.createElement
       "span" #j {:className "anchor"}
       (sablono.interpreter/interpret (:name datum)))))))

(deftest test-issue-2-merge-class
  (are-html-expanded
   '[:div.a {:class (if (true? true) "true" "false")}]
   '(js/React.createElement
     "div" #j {:className (sablono.util/join-classes ["a" (if (true? true) "true" "false")])})
   '[:div.a.b {:class (if (true? true) ["true"] "false")}]
   '(js/React.createElement
     "div" #j {:className (sablono.util/join-classes ["a" "b" (if (true? true) ["true"] "false")])})))

(deftest test-issue-3-recursive-js-literal
  (are-html-expanded
   '[:div.interaction-row {:style {:position "relative"}}]
   '(js/React.createElement "div" #j {:className "interaction-row", :style #j {:position "relative"}}))
  (let [username "foo", hidden #(if %1 {:display "none"} {:display "block"})]
    (are-html-expanded
     '[:ul.nav.navbar-nav.navbar-right.pull-right
       [:li.dropdown {:style (hidden (nil? username))}
        [:a.dropdown-toggle {:role "button" :href "#"} (str "Welcome, " username)
         [:span.caret]]
        [:ul.dropdown-menu {:role "menu" :style {:left 0}}]]]
     '(js/React.createElement
       "ul" #j {:className "nav navbar-nav navbar-right pull-right"}
       (js/React.createElement
        "li" #j {:style (clj->js (hidden (nil? username))), :className "dropdown"}
        (js/React.createElement
         "a" #j {:href "#", :role "button", :className "dropdown-toggle"}
         (sablono.interpreter/interpret (str "Welcome, " username))
         (js/React.createElement
          "span" #j {:className "caret"}))
        (js/React.createElement
         "ul" #j {:role "menu", :style #j {:left 0}, :className "dropdown-menu"}))))))

(deftest test-issue-22-id-after-class
  (are-html-expanded
   [:div.well#setup]
   '(js/React.createElement "div" #j {:id "setup", :className "well"})))

(deftest test-issue-25-comma-separated-class
  (are-html-expanded
   '[:div.c1.c2 "text"]
   '(js/React.createElement "div" #j {:className "c1 c2"} "text")
   '[:div.aa (merge {:class "bb"})]
   '(let* [attrs (merge {:class "bb"})]
      (clojure.core/apply
       js/React.createElement "div"
       (if (clojure.core/map? attrs)
         (sablono.interpreter/attributes
          (sablono.normalize/merge-with-class {:class ["aa"]} attrs))
         #j {:className "aa"})
       (if (clojure.core/map? attrs)
         nil [(sablono.interpreter/interpret attrs)])))))

(deftest test-issue-33-number-warning
  (are-html-expanded
   '[:div (count [1 2 3])]
   '(let* [attrs (count [1 2 3])]
      (clojure.core/apply
       js/React.createElement "div"
       (if (clojure.core/map? attrs)
         (sablono.interpreter/attributes attrs)
         nil)
       (if (clojure.core/map? attrs)
         nil [(sablono.interpreter/interpret attrs)])))))

(deftest test-issue-37-camel-case-style-attrs
  (are-html-expanded
   '[:div {:style {:z-index 1000}}]
   '(js/React.createElement "div" #j {:style #j {:zIndex 1000}})))

(deftest shorthand-div-forms
  (are-html-expanded
   [:#test]
   '(js/React.createElement "div" #j {:id "test"})
   '[:.klass]
   '(js/React.createElement "div" #j {:className "klass"})
   '[:#test.klass]
   '(js/React.createElement "div" #j {:id "test" :className "klass"})
   '[:#test.klass1.klass2]
   '(js/React.createElement "div" #j {:id "test" :className "klass1 klass2"})
   '[:.klass1.klass2#test]
   '(js/React.createElement "div" #j {:id "test" :className "klass1 klass2"})))

(deftest test-namespaced-fn-call
  (are-html-expanded
   '(some-ns/comp "arg")
   '(sablono.interpreter/interpret (some-ns/comp "arg"))
   '(some.ns/comp "arg")
   '(sablono.interpreter/interpret (some.ns/comp "arg"))))

(deftest test-compile-div-with-nested-lazy-seq
  (is (= (compile [:div (map identity ["A" "B"])])
         '(let* [attrs (map identity ["A" "B"])]
            (clojure.core/apply
             js/React.createElement "div"
             (if (clojure.core/map? attrs)
               (sablono.interpreter/attributes attrs)
               nil)
             (if (clojure.core/map? attrs)
               nil
               [(sablono.interpreter/interpret attrs)]))))))

(deftest test-compile-div-with-nested-list
  (is (= (compile [:div '("A" "B")])
         '(js/React.createElement "div" nil "A" "B"))))

(deftest test-compile-div-with-nested-vector
  (is (= (compile [:div ["A" "B"]])
         '(js/React.createElement "div" nil "A" "B")))
  (is (= (compile [:div (vector "A" "B")])
         '(let* [attrs (vector "A" "B")]
            (clojure.core/apply
             js/React.createElement "div"
             (if (clojure.core/map? attrs)
               (sablono.interpreter/attributes attrs)
               nil)
             (if (clojure.core/map? attrs)
               nil
               [(sablono.interpreter/interpret attrs)]))))))

(deftest test-class-as-set
  (is (= (compile [:div.a {:class #{"a" "b" "c"}}])
         '(js/React.createElement "div" #j {:className "a a b c"}))))

(deftest test-class-as-list
  (is (= (compile [:div.a {:class (list "a" "b" "c")}])
         '(js/React.createElement "div" #j {:className (sablono.util/join-classes ["a" (list "a" "b" "c")])}))))

(deftest test-class-as-vector
  (is (= (compile [:div.a {:class (vector "a" "b" "c")}])
         '(js/React.createElement
           "div" #j {:className (sablono.util/join-classes ["a" (vector "a" "b" "c")])}))))

(deftest test-class-merge-symbol
  (let [class #{"b"}]
    (are-html-expanded
     [:div.a {:class class}]
     '(js/React.createElement "div" #j {:className "a b"}))))

(deftest test-issue-90
  (is (= (compile [:div nil (case :a :a "a")])
         '(js/React.createElement
           "div" nil nil
           (sablono.interpreter/interpret
            (case :a :a "a"))))))

(deftest test-compile-attr-class
  (are [form expected]
      (= expected (compile-attr :class form))
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
  (is (= (compile (let [x "x"] [:div "x"]))
         '(let* [x "x"] (js/React.createElement "div" nil "x")))))

(deftest test-optimize-for-loop
  (is (= (compile [:ul (for [n (range 3)] [:li n])])
         '(js/React.createElement
           "ul" nil
           (into-array
            (clojure.core/for [n (range 3)]
              (clojure.core/let [attrs n]
                (clojure.core/apply
                 js/React.createElement "li"
                 (if (clojure.core/map? attrs)
                   (sablono.interpreter/attributes attrs)
                   nil)
                 (if (clojure.core/map? attrs)
                   nil [(sablono.interpreter/interpret attrs)]))))))))
  (is (= (compile [:ul (for [n (range 3)] [:li ^:attrs n])])
         '(js/React.createElement
           "ul" nil
           (into-array
            (clojure.core/for [n (range 3)]
              (clojure.core/let [attrs n]
                (clojure.core/apply
                 js/React.createElement "li"
                 (sablono.interpreter/attributes attrs) nil))))))))

(deftest test-optimize-if
  (is (= (compile (if true [:span "foo"] [:span "bar"]) )
         '(if true
            (js/React.createElement "span" nil "foo")
            (js/React.createElement "span" nil "bar")))))

(deftest test-issue-115
  (is (= (compile [:a {:id :XY}])
         '(js/React.createElement "a" #j {:id "XY"}))))

(deftest test-issue-130
  (let [css {:table-cell "bg-blue"}]
    (is (= (compile [:div {:class (:table-cell css)} [:span "abc"]])
           '(js/React.createElement
             "div"
             #j {:className (sablono.util/join-classes [(:table-cell css)])}
             (js/React.createElement "span" nil "abc"))))))

(deftest test-issue-141-inline
  (testing "with attributes"
    (is (= (compile [:span {} ^:inline (constantly 1)])
           '(js/React.createElement "span" nil (constantly 1)))))
  (testing "without attributes"
    (is (= (compile [:span ^:inline (constantly 1)])
           '(js/React.createElement "span" nil (constantly 1))))))

(deftest test-compile-attributes-non-literal-key
  (is (= (compile [:input {(case :checkbox :checkbox :checked :value) "x"}])
         '(sablono.interpreter/create-element
           "input" (sablono.interpreter/attributes
                    {(case :checkbox :checkbox :checked :value) "x"})))))
