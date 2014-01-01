(ns sablono.compiler-test
  (:require [clojure.test :refer :all]
            [clojure.walk :refer [prewalk]]
            [sablono.core :refer [html html-expand]]
            [sablono.compiler :refer :all])
  (:import cljs.tagged_literals.JSValue))

(defn replace-js-literals [forms]
  (prewalk
   (fn [form]
     (if (instance? JSValue form)
       (.val form) form))
   forms))

(defmacro are-html-expanded [& body]
  `(are [form# expected#]
     (is (= (replace-js-literals expected#)
            (replace-js-literals (html-expand form#))))
     ~@body))

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

(deftest test-multiple-children
  (is (= (replace-js-literals
          '(into-array [(js/React.DOM.div #js {:id "a"})
                        (js/React.DOM.div #js {:id "b"})]))
         (replace-js-literals
          (html-expand [:div#a] [:div#b])))))

(deftest tag-names
  (testing "basic tags"
    (are-html-expanded
     [:div] '(js/React.DOM.div #js {})
     [:div] '(js/React.DOM.div #js {})
     ["div"] '(js/React.DOM.div #js {})
     ['div] '(sablono.render/render-element [div])))
  (testing "tag syntax sugar"
    (are-html-expanded
     [:div#foo] '(js/React.DOM.div #js {:id "foo"})
     [:div.foo] '(js/React.DOM.div #js {:className "foo"})
     [:div.foo (str "bar" "baz")] '(js/React.DOM.div #js {:className "foo"} "barbaz")
     [:div.a.b] '(js/React.DOM.div #js {:className "a b"})
     [:div.a.b.c] '(js/React.DOM.div #js {:className "a b c"})
     [:div#foo.bar.baz] '(js/React.DOM.div #js {:id "foo", :className "bar baz"}))))

(deftest tag-contents
  (testing "empty tags"
    (are-html-expanded
     [:div] '(js/React.DOM.div #js {})
     [:h1] '(js/React.DOM.h1 #js {})
     [:script] '(js/React.DOM.script #js {})
     [:text] '(js/React.DOM.text #js {})
     [:a] '(js/React.DOM.a #js {})
     [:iframe] '(js/React.DOM.iframe #js {})
     [:title] '(js/React.DOM.title #js {})
     [:section] '(js/React.DOM.section #js {})))
  (testing "tags containing text"
    (are-html-expanded
     [:text "Lorem Ipsum"] '(js/React.DOM.text #js {} "Lorem Ipsum")))
  (testing "contents are concatenated"
    (are-html-expanded
     [:div "foo" "bar"]
     '(js/React.DOM.div #js {} "foo" "bar")
     [:div [:p] [:br]]
     '(js/React.DOM.div
       #js {}
       (js/React.DOM.p #js {})
       (js/React.DOM.br #js {}))))
  (testing "seqs are expanded"
    ;; (is (= (html [:div (list "foo" "bar")]) "<div><span>foo</span><span>bar</span></div>"))
    ;; (is (= (html (list [:p "a"] [:p "b"])) "<p>a</p><p>b</p>"))
    )
  (testing "vecs don't expand - error if vec doesn't have tag name"
    (is (thrown? Exception (html (vector [:p "a"] [:p "b"])))))
  (testing "tags can contain tags"
    (are-html-expanded
     [:div [:p]]
     '(js/React.DOM.div #js {} (js/React.DOM.p #js {}))
     [:div [:b]]
     '(js/React.DOM.div #js {} (js/React.DOM.b #js {}))
     [:p [:span [:a "foo"]]]
     '(js/React.DOM.p #js {} (js/React.DOM.span #js {} (js/React.DOM.a #js {} "foo"))))))

(deftest tag-attributes
  (testing "tag with blank attribute map"
    (are-html-expanded
     [:div {}] '(js/React.DOM.div #js {})))
  (testing "tag with populated attribute map"
    (are-html-expanded
     [:div {:min "1", :max "2"}] '(js/React.DOM.div #js {:min "1", :max "2"})
     [:img {"id" "foo"}] '(js/React.DOM.img #js {"id" "foo"})
     [:img {:id "foo"}] '(js/React.DOM.img #js {:id "foo"})
     ;; [:img {'id "foo"}] '(js/React.DOM.img #js {id "foo"})
     ;; [:div {:a "1", 'b "2", "c" "3"}] '(js/React.DOM.div #js {b "2", :a "1", "c" "3"})
     ))
  (testing "attribute values are escaped"
    (are-html-expanded
     [:div {:id "\""}] '(js/React.DOM.div #js {:id "\""})))
  (testing "boolean attributes"
    (are-html-expanded
     [:input {:type "checkbox" :checked true}]
     '(js/React.DOM.input #js {:checked true, :type "checkbox"})
     [:input {:type "checkbox" :checked false}]
     '(js/React.DOM.input #js {:type "checkbox"})))
  (testing "nil attributes"
    (are-html-expanded
     [:span {:class nil} "foo"] '(js/React.DOM.span #js {} "foo"))))

(deftest compiled-tags
  (testing "tag content can be vars"
    (let [x "foo"]
      (are-html-expanded
       [:span x] '(js/React.DOM.span #js {} "foo"))))
  (testing "tag content can be forms"
    (are-html-expanded
     [:span (str (+ 1 1))] '(js/React.DOM.span #js {} "2")
     [:span ({:foo "bar"} :foo)] '(js/React.DOM.span #js {} "bar")))
  (testing "attributes can contain vars"
    (let [id "id"]
      (are-html-expanded
       [:div {:id id}] '(js/React.DOM.div #js {:id "id"})
       [:div {id "id"}] '(js/React.DOM.div #js {"id" "id"})
       [:div {:id id} "bar"] '(js/React.DOM.div #js {:id "id"} "bar"))))
  (testing "attributes are evaluated"
    (are-html-expanded
     [:img {:src (str "/foo" "/bar")}] '(js/React.DOM.img #js {:src "/foo/bar"})
     [:div {:id (str "a" "b")} (str "foo")] '(js/React.DOM.div #js {:id "ab"} "foo")))
  (testing "type hints"
    (let [string "x"]
      (are-html-expanded
       [:span ^String string] '(js/React.DOM.span #js {} "x"))))
  (testing "optimized forms"
    (are-html-expanded
     ;; [:ul (for [n (range 3)] [:li n])]
     ;; "<ul><li>0</li><li>1</li><li>2</li></ul>"
     [:div (if true [:span "foo"] [:span "bar"])]
     '(js/React.DOM.div #js {} (js/React.DOM.span #js {} "foo"))))
  (testing "values are evaluated only once"
    (let [times-called (atom 0)
          foo #(swap! times-called inc)]
      (html-expand [:div (foo)])
      (is (= @times-called 1)))))

(deftest test-benchmark-template
  (let [datum {:key "key" :name "name"}]
    (are-html-expanded
     [:li
      [:a {:href (str "#show/" (:key datum))}]
      [:div {:id (str "item" (:key datum))
             :className ["class1" "class2"]}
       [:span {:className "anchor"} (:name datum)]]]
     '(js/React.DOM.li
       #js {}
       (js/React.DOM.a #js {:href "#show/key"})
       (js/React.DOM.div
        #js {:className "class1 class2", :id "itemkey"}
        (js/React.DOM.span {:className "anchor"} "name"))))))

(deftest test-issue-2-merge-classname
  (are-html-expanded
   '[:div.a {:className (if (true? true) "true" "false")}]
   '(js/React.DOM.div #js {:className (sablono.util/join-classes ["a" (if (true? true) "true" "false")])})
   '[:div.a.b {:className (if (true? true) ["true"] "false")}]
   '(js/React.DOM.div #js {:className (sablono.util/join-classes ["a" "b" (if (true? true) ["true"] "false")])})))

(deftest test-issue-3-recursive-js-value
  (are-html-expanded
   [:div.interaction-row {:style {:position "relative"}}]
   '(js/React.DOM.div #js {:className "interaction-row", :style #js {:position "relative"}})))
