(ns sablono.compiler-test
  (:refer-clojure :exclude [compile])
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]
            [clojure.test :refer :all]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.properties :as prop]
            [clojure.walk :refer [prewalk]]
            [sablono.compiler :refer :all]
            [sablono.core :refer [attrs html html-expand]]
            [sablono.interpreter :as interpreter])
  (:import cljs.tagged_literals.JSValue))

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
  (.write w "#js ")
  (.write w (pr-str (.val v))))

(defn wrap-js-value [forms]
  (prewalk
   (fn [form]
     (if (instance? JSValue form)
       (JSValueWrapper. (wrap-js-value (.val form))) form))
   forms))

(defmacro with-static-gensym [& body]
  `(with-redefs [gensym (fn [& [prefix#]] (symbol (str prefix# "1")))]
     ~@body))

(defmacro compile [form]
  `(wrap-js-value (macroexpand '(html ~form))))

(defn js [x]
  (JSValueWrapper. (wrap-js-value x)))

(defmacro are-html-expanded [& body]
  `(are [form# expected#]
       (is (= (wrap-js-value expected#)
              (wrap-js-value (html-expand form#))))
     ~@body))

(deftest test-compile-attrs-js
  (are [attrs expected]
      (= (wrap-js-value expected)
         (wrap-js-value (compile-attrs-js attrs)))
    nil nil
    {:class "my-class"}
    #js {:className "my-class"}
    {:class '(identity "my-class")}
    #js {:className (sablono.util/join-classes (identity "my-class"))}
    {:class "my-class"
     :style {:background-color "black"}}
    #js {:className "my-class"
         :style #js {:backgroundColor "black"}}
    {:class '(identity "my-class")
     :style {:background-color '(identity "black")}}
    #js {:className (sablono.util/join-classes (identity "my-class"))
         :style #js {:backgroundColor (identity "black")}}
    {:id :XY}
    #js {:id "XY"}))

(deftest test-attrs
  (are [form expected]
      (= (wrap-js-value expected)
         (wrap-js-value (attrs form)))
    nil nil
    {:class "my-class"}
    #js {:className "my-class"}
    {:class ["a" "b"]}
    #js {:className "a b"}
    {:class '(identity "my-class")}
    #js {:className (sablono.util/join-classes '(identity "my-class"))}
    {:class "my-class"
     :style {:background-color "black"}}
    #js {:className "my-class"
         :style #js {:backgroundColor "black"}}
    {:class '(identity "my-class")
     :style {:background-color '(identity "black")}}
    #js {:className (sablono.util/join-classes '(identity "my-class"))
         :style #js {:backgroundColor '(identity "black")}}))

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

(defspec test-tag-empty
  (prop/for-all
   [tag (s/gen keyword?)]
   (= (eval `(compile [~tag]))
      (js {:$$typeof sablono.core/react-element-sym
           :type (name tag)
           :props (js {})}))))

(deftest test-tag-keyword
  (is (= (compile [:div])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props #js {}}))))

(deftest test-tag-string
  (is (= (compile ["div"])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props #js {}}))))

(deftest test-tag-symbol
  (is (= (compile ['div])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {})}))))

(deftest test-tag-syntax-sugar-id
  (is (= (compile [:div#foo])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:id "foo"})}))))

(deftest test-tag-syntax-sugar-class
  (is (= (compile [:div.foo])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:className "foo"})}))))

(deftest test-tag-syntax-sugar-class-multiple
  (is (= (compile [:div.a.b.c])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:className "a b c"})}))))

(deftest test-tag-syntax-sugar-id-class
  (is (= (compile [:div#a.b.c])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:id "a" :className "b c"})}))))

(deftest test-tag-key-all-literal
  (is (= (compile [:div {:key 1}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :key "1"
              :props #js {}}))))

(deftest test-tag-key-literal-tag-and-attributes
  (is (= (compile [:div {:key 1} (str "x")])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:children '(sablono.interpreter/interpret (str "x"))})
              :key "1"}))))

(deftest test-tag-ref-all-literal
  (is (= (compile [:div {:ref "node"}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :ref "node"
              :props #js {}}))))

(deftest test-tag-ref-literal-tag-and-attributes
  (is (= (compile [:div {:ref "node"} (str "x")])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:children '(sablono.interpreter/interpret (str "x"))})
              :ref "node"}))))

(deftest tag-tag-syntax-sugar-class-fn-child
  (with-static-gensym
    (is (= (compile [:div.foo (str "bar" "baz")])
           (wrap-js-value
            '(let* [attrs1 (str "bar" "baz")]
               #js {:$$typeof sablono.core/react-element-sym
                    :type "div"
                    :props (sablono.interpreter/props
                            (if (clojure.core/map? attrs1)
                              (sablono.interpreter/attributes
                               (sablono.normalize/merge-with-class
                                {:class ["foo"]} attrs1))
                              #js {:className "foo"})
                            (if (clojure.core/map? attrs1)
                              nil #js [(sablono.interpreter/interpret attrs1)]))}))))))

(deftest test-tag-content-text
  (is (= (compile [:text "Lorem Ipsum"])
         (js {:$$typeof sablono.core/react-element-sym
              :type "text"
              :props (js {:children "Lorem Ipsum"})}))))

(deftest test-tag-content-concat-string
  (is (= (compile [:div "foo" "bar"])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:children (js ["foo" "bar"])})}))))

(deftest test-tag-content-concat-elements
  (is (= (compile [:div [:p] [:br]])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:children
                          (js
                           [(js {:$$typeof sablono.core/react-element-sym
                                 :type "p"
                                 :props #js {}})
                            (js {:$$typeof sablono.core/react-element-sym
                                 :type "br"
                                 :props #js {}})])})}))))

(deftest test-tag-content-seqs-are-expanded
  (with-static-gensym
    (is (= (compile [:div (list "foo" "bar")])
           (wrap-js-value
            '(let* [attrs1 (list "foo" "bar")]
               #js {:$$typeof sablono.core/react-element-sym
                    :type "div"
                    :props (sablono.interpreter/props
                            (if (clojure.core/map? attrs1)
                              (sablono.interpreter/attributes attrs1) nil)
                            (if (clojure.core/map? attrs1)
                              nil #js [(sablono.interpreter/interpret attrs1)]))}))))))

(deftest test-interpret-list
  (is (= (compile (list [:p "a"] [:p "b"]))
         '(sablono.interpreter/interpret (list [:p "a"] [:p "b"])))))

(deftest tag-content-vectors-dont-expand
  (is (thrown? Exception (html (vector [:p "a"] [:p "b"])))))

(deftest tag-content-can-contain-tags
  (testing "tags can contain tags"
    (are-html-expanded
     '[:div [:p]]
     (js {:$$typeof sablono.core/react-element-sym
          :type "div"
          :props
          (js {:children
               (js {:$$typeof sablono.core/react-element-sym
                    :type "p"
                    :props (js {})})})})

     '[:div [:b]]
     (js {:$$typeof sablono.core/react-element-sym
          :type "div"
          :props
          (js {:children
               (js {:$$typeof sablono.core/react-element-sym
                    :type "b"
                    :props (js {})})})})


     '[:p [:span [:a "foo"]]]
     (js {:$$typeof sablono.core/react-element-sym
          :type "p"
          :props
          (js {:children
               (js {:$$typeof sablono.core/react-element-sym
                    :type "span"
                    :props
                    (js {:children
                         (js {:$$typeof sablono.core/react-element-sym
                              :type "a"
                              :props
                              (js {:children "foo"})})})})})}))))

(deftest test-tag-attributes-tag-with-empty-attribute-map
  (is (= (compile [:div {}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {})}))))

(deftest test-tag-attributes-tag-with-populated-attribute-map
  (is (= (compile [:div {:min "1", :max "2"}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:min "1", :max "2"})})))

  (is (= (compile [:img {"id" "foo"}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "img"
              :props (js {"id" "foo"})})))

  (is (= (compile [:img {:id "foo"}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "img"
              :props (js {:id "foo"})}))))

(deftest test-tag-attributes-attribute-values-are-escaped
  (is (= (compile [:div {:id "\""}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:id "\""})}))))

(deftest test-tag-attributes-attributes-are-converted-to-their-DOM-equivalents
  (are-html-expanded
   '[:div {:class "classy"}]
   (js {:$$typeof sablono.core/react-element-sym
        :type "div"
        :props (js {:className "classy"})})

   '[:div {:data-foo-bar "baz"}]
   (js {:$$typeof sablono.core/react-element-sym
        :type "div"
        :props (js {:data-foo-bar "baz"})})

   '[:label {:for "foo"}]
   (js {:$$typeof sablono.core/react-element-sym
        :type "label"
        :props (js {:htmlFor "foo"})})))

(deftest test-tag-attributes-boolean-attributes
  (are-html-expanded
   '[:input {:type "checkbox" :checked true}]
   '(sablono.interpreter/create-element "input" #js {:checked true, :type "checkbox"})
   '[:input {:type "checkbox" :checked false}]
   '(sablono.interpreter/create-element "input" #js {:checked false, :type "checkbox"})))

(deftest test-tag-attributes-nil-attributes
  (is (= (compile [:span {:class nil} "foo"])
         (js {:$$typeof sablono.core/react-element-sym
              :type "span"
              :props (js {:className nil, :children "foo"})}))))

(deftest test-tag-attributes-empty-attributes
  (is (= (compile [:span {} "foo"])
         (js {:$$typeof sablono.core/react-element-sym
              :type "span"
              :props (js {:children "foo"})}))))

(deftest test-tag-attributes-tag-with-aria-attributes
  (is (= (compile [:div {:aria-disabled true}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:aria-disabled true})}))))

(deftest test-tag-attributes-tag-with-data-attributes
  (is (= (compile [:div {:data-toggle "modal" :data-target "#modal"}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:data-toggle "modal", :data-target "#modal"})}))))

(deftest test-issue-22-id-after-class
  (is (= (compile [:div.well#setup])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:id "setup"
                          :className "well"})}))))

(deftest test-issue-25-comma-separated-class
  (is (= (compile [:div.c1.c2 "text"])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:className "c1 c2"
                          :children "text"})}))))

(deftest test-issue-25-comma-separated-class-interpret
  (with-static-gensym
    (is (= (compile [:div.aa (merge {:class "bb"})])
           (wrap-js-value
            '(let* [attrs1 (merge {:class "bb"})]
               #js {:$$typeof sablono.core/react-element-sym,
                    :type "div"
                    :props (sablono.interpreter/props
                            (if (clojure.core/map? attrs1)
                              (sablono.interpreter/attributes
                               (sablono.normalize/merge-with-class
                                {:class ["aa"]} attrs1))
                              #js {:className "aa"})
                            (if (clojure.core/map? attrs1)
                              nil #js [(sablono.interpreter/interpret attrs1)]))}))))))

(deftest test-issue-33-number-warning
  (with-static-gensym
    (is (= (compile [:div (count [1 2 3])])
           (wrap-js-value
            '(let* [attrs1 (count [1 2 3])]
               #js {:$$typeof sablono.core/react-element-sym
                    :type "div"
                    :props (sablono.interpreter/props
                            (if (clojure.core/map? attrs1)
                              (sablono.interpreter/attributes attrs1) nil)
                            (if (clojure.core/map? attrs1)
                              nil #js [(sablono.interpreter/interpret attrs1)]))}))))))

(deftest test-issue-37-camel-case-style-attrs
  (are-html-expanded
   '[:div {:style {:z-index 1000}}]
   #js {:$$typeof sablono.core/react-element-sym
        :type "div"
        :props #js {:style #js {:zIndex 1000}}}))

(deftest shorthand-div-forms
  (are-html-expanded
   [:#test]
   #js {:$$typeof sablono.core/react-element-sym
        :type "div"
        :props #js {:id "test"}}

   '[:.klass]
   #js {:$$typeof sablono.core/react-element-sym
        :type "div"
        :props #js {:className "klass"}}

   '[:#test.klass]
   #js {:$$typeof sablono.core/react-element-sym
        :type "div"
        :props #js {:id "test"
                    :className "klass"}}

   '[:#test.klass1.klass2]
   #js {:$$typeof sablono.core/react-element-sym
        :type "div"
        :props #js {:id "test"
                    :className "klass1 klass2"}}

   '[:.klass1.klass2#test]
   #js {:$$typeof sablono.core/react-element-sym
        :type "div"
        :props #js {:id "test"
                    :className "klass1 klass2"}}))

(deftest test-namespaced-fn-call
  (are-html-expanded
   '(some-ns/comp "arg")
   '(sablono.interpreter/interpret (some-ns/comp "arg"))
   '(some.ns/comp "arg")
   '(sablono.interpreter/interpret (some.ns/comp "arg"))))

(deftest test-compile-div-with-nested-lazy-seq
  (with-static-gensym
    (is (= (compile [:div (map identity ["A" "B"])])
           (wrap-js-value
            '(let* [attrs1 (map identity ["A" "B"])]
               #js {:$$typeof sablono.core/react-element-sym
                    :type "div"
                    :props
                    (sablono.interpreter/props
                     (if (clojure.core/map? attrs1)
                       (sablono.interpreter/attributes attrs1) nil)
                     (if (clojure.core/map? attrs1)
                       nil #js [(sablono.interpreter/interpret attrs1)]))}))))))

(deftest test-compile-div-with-nested-list
  (is (= (compile [:div '("A" "B")])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:children (js ["A" "B"])})}))))

(deftest test-compile-div-with-nested-vector
  (is (= (compile [:div ["A" "B"]])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:children #js ["A" "B"]})}))))

(deftest test-compile-div-with-nested-vector-dynamic
  (with-static-gensym
    (is (= (compile [:div (vector "A" "B")])
           (wrap-js-value
            '(let* [attrs1 (vector "A" "B")]
               #js {:$$typeof sablono.core/react-element-sym
                    :type "div"
                    :props
                    (sablono.interpreter/props
                     (if (clojure.core/map? attrs1)
                       (sablono.interpreter/attributes attrs1) nil)
                     (if (clojure.core/map? attrs1)
                       nil #js [(sablono.interpreter/interpret attrs1)]))}))))))

(deftest test-class-as-set
  (is (= (compile [:div.a {:class #{"a" "b" "c"}}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:className "a a b c"})}))))

(deftest test-class-as-list
  (is (= (compile [:div.a {:class (list "a" "b" "c")}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props
              (js {:className '(sablono.util/join-classes
                                ["a" (list "a" "b" "c")])})}))))

(deftest test-class-as-vector
  (is (= (compile [:div.a {:class (vector "a" "b" "c")}])
         (wrap-js-value
          #js {:$$typeof sablono.core/react-element-sym
               :type "div"
               :props #js {:className (sablono.util/join-classes ["a" (vector "a" "b" "c")])}}))))

(deftest test-class-merge-symbol
  (let [class #{"b"}]
    (is (= (eval `(compile [:div.a {:class ~class}]))
           (js {:$$typeof sablono.core/react-element-sym
                :type "div"
                :props (js {:className "a b"})})))))

(deftest test-issue-90
  (is (= (compile [:div nil (case :a :a "a")])
         (js {:$$typeof sablono.core/react-element-sym
              :type "div"
              :props (js {:children (js [nil '(sablono.interpreter/interpret (case :a :a "a"))])})}))))

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
         (wrap-js-value
          '(let* [x "x"]
             #js {:$$typeof sablono.core/react-element-sym
                  :type "div"
                  :props #js {:children "x"}})))))

(deftest test-optimize-for-loop
  (with-static-gensym
    (is (= (compile [:ul (for [n (range 3)] [:li n])])
           (wrap-js-value
            #js {:$$typeof sablono.core/react-element-sym
                 :type "ul"
                 :props
                 #js {:children
                      (into-array
                       (clojure.core/for [n (range 3)]
                         (clojure.core/let [attrs1 n]
                           #js {:$$typeof sablono.core/react-element-sym
                                :type "li"
                                :props (sablono.interpreter/props
                                        (if (clojure.core/map? attrs1)
                                          (sablono.interpreter/attributes attrs1) nil)
                                        (if (clojure.core/map? attrs1)
                                          nil #js [(sablono.interpreter/interpret attrs1)]))})))}})))))

(deftest test-optimize-for-loop-hint
  (with-static-gensym
    (is (= (compile [:ul (for [n (range 3)] [:li ^:attrs n])])
           (wrap-js-value
            #js {:$$typeof sablono.core/react-element-sym
                 :type "ul"
                 :props
                 #js {:children
                      (into-array
                       (clojure.core/for [n (range 3)]
                         #js {:$$typeof sablono.core/react-element-sym
                              :type "li"
                              :props (sablono.interpreter/props (sablono.interpreter/attributes n) nil)}))}})))))

(deftest test-optimize-if
  (is (= (compile (if true [:span "foo"] [:span "bar"]) )
         (wrap-js-value
          '(if true
             #js {:$$typeof sablono.core/react-element-sym
                  :type "span"
                  :props #js {:children "foo"}}
             #js {:$$typeof sablono.core/react-element-sym
                  :type "span"
                  :props #js {:children "bar"}})))))

(deftest test-issue-115
  (is (= (compile [:a {:id :XY}])
         (js {:$$typeof sablono.core/react-element-sym
              :type "a"
              :props (js {:id "XY"})}))))

(deftest test-issue-130
  (let [css {:table-cell "bg-blue"}]
    (is (= (compile [:div {:class (:table-cell css)} [:span "abc"]])
           (js {:$$typeof sablono.core/react-element-sym,
                :type "div"
                :props (js {:className '(sablono.util/join-classes [(:table-cell css)])
                            :children (js {:$$typeof sablono.core/react-element-sym
                                           :type "span"
                                           :props (js {:children "abc"})})})})))))

(deftest test-issue-141-inline
  (testing "with attributes"
    (is (= (compile [:span {:class "a"} ^:inline (constantly 1)])
           (js {:$$typeof sablono.core/react-element-sym
                :type "span"
                :props (js {:className "a"
                            :children '(constantly 1)})}))))
  (testing "without attributes"
    (is (= (compile [:span ^:inline (constantly 1)])
           (js {:$$typeof sablono.core/react-element-sym
                :type "span"
                :props (js {:children '(constantly 1)})})))))

(deftest test-compile-attributes-non-literal-key
  (is (= (compile [:input {(case :checkbox :checkbox :checked :value) "x"}])
         '(sablono.interpreter/create-element
           "input" (sablono.interpreter/attributes
                    {(case :checkbox :checkbox :checked :value) "x"})))))
