(ns sablono.compiler-test
  (:require [clojure.test :refer :all]
            [sablono.core :refer [html html-expand]]))

(defmacro are-html-expanded [& body]
  `(are [form# expected#]
     (is (= expected# (html-expand form#)))
     ~@body))

(deftest test-multiple-children
  (is (= '(into-array [(js/React.DOM.div (cljs.core/clj->js {:id "a"}))
                       (js/React.DOM.div (cljs.core/clj->js {:id "b"}))])
         (html-expand [:div#a] [:div#b]))))

(deftest tag-names
  (testing "basic tags"
    (are-html-expanded
     [:div] '(js/React.DOM.div (cljs.core/clj->js {}))
     [:div] '(js/React.DOM.div (cljs.core/clj->js {}))
     ["div"] '(js/React.DOM.div (cljs.core/clj->js {}))
     ['div] '(sablono.render/render-element [div])))
  (testing "tag syntax sugar"
    (are-html-expanded
     [:div#foo] '(js/React.DOM.div (cljs.core/clj->js {:id "foo"}))
     [:div.foo] '(js/React.DOM.div (cljs.core/clj->js {:className "foo"}))
     [:div.foo (str "bar" "baz")] '(js/React.DOM.div (cljs.core/clj->js {:className "foo"}) "barbaz")
     [:div.a.b] '(js/React.DOM.div (cljs.core/clj->js {:className "a b"}))
     [:div.a.b.c] '(js/React.DOM.div (cljs.core/clj->js {:className "a b c"}))
     [:div#foo.bar.baz] '(js/React.DOM.div (cljs.core/clj->js {:id "foo", :className "bar baz"})))))

(deftest tag-contents
  (testing "empty tags"
    (are-html-expanded
     [:div] '(js/React.DOM.div (cljs.core/clj->js {}))
     [:h1] '(js/React.DOM.h1 (cljs.core/clj->js {}))
     [:script] '(js/React.DOM.script (cljs.core/clj->js {}))
     [:text] '(js/React.DOM.text (cljs.core/clj->js {}))
     [:a] '(js/React.DOM.a (cljs.core/clj->js {}))
     [:iframe] '(js/React.DOM.iframe (cljs.core/clj->js {}))
     [:title] '(js/React.DOM.title (cljs.core/clj->js {}))
     [:section] '(js/React.DOM.section (cljs.core/clj->js {}))))
  (testing "tags containing text"
    (are-html-expanded
     [:text "Lorem Ipsum"] '(js/React.DOM.text (cljs.core/clj->js {}) "Lorem Ipsum")))
  (testing "contents are concatenated"
    (are-html-expanded
     [:div "foo" "bar"]
     '(js/React.DOM.div (cljs.core/clj->js {}) "foo" "bar")
     [:div [:p] [:br]]
     '(js/React.DOM.div
       (cljs.core/clj->js {})
       (js/React.DOM.p (cljs.core/clj->js {}))
       (js/React.DOM.br (cljs.core/clj->js {})))))
  (testing "seqs are expanded"
    ;; (is (= (html [:div (list "foo" "bar")]) "<div><span>foo</span><span>bar</span></div>"))
    ;; (is (= (html (list [:p "a"] [:p "b"])) "<p>a</p><p>b</p>"))
    )
  (testing "vecs don't expand - error if vec doesn't have tag name"
    (is (thrown? Exception (html (vector [:p "a"] [:p "b"])))))
  (testing "tags can contain tags"
    (are-html-expanded
     [:div [:p]]
     '(js/React.DOM.div (cljs.core/clj->js {}) (js/React.DOM.p (cljs.core/clj->js {})))
     [:div [:b]]
     '(js/React.DOM.div (cljs.core/clj->js {}) (js/React.DOM.b (cljs.core/clj->js {})))
     [:p [:span [:a "foo"]]]
     '(js/React.DOM.p (cljs.core/clj->js {}) (js/React.DOM.span (cljs.core/clj->js {}) (js/React.DOM.a (cljs.core/clj->js {}) "foo"))))))

(deftest tag-attributes
  (testing "tag with blank attribute map"
    (are-html-expanded
     [:div {}] '(js/React.DOM.div (cljs.core/clj->js {}))))
  (testing "tag with populated attribute map"
    (are-html-expanded
     [:div {:min "1", :max "2"}] '(js/React.DOM.div (cljs.core/clj->js {:min "1", :max "2"}))
     [:img {"id" "foo"}] '(js/React.DOM.img (cljs.core/clj->js {"id" "foo"}))
     [:img {:id "foo"}] '(js/React.DOM.img (cljs.core/clj->js {:id "foo"}))
     [:img {'id "foo"}] '(js/React.DOM.img (cljs.core/clj->js {id "foo"}))
     [:div {:a "1", 'b "2", "c" "3"}] '(js/React.DOM.div (cljs.core/clj->js {b "2", :a "1", "c" "3"}))))
  (testing "attribute values are escaped"
    (are-html-expanded
     [:div {:id "\""}] '(js/React.DOM.div (cljs.core/clj->js {:id "\""}))))
  (testing "boolean attributes"
    (are-html-expanded
     [:input {:type "checkbox" :checked true}]
     '(js/React.DOM.input (cljs.core/clj->js {:checked true, :type "checkbox"}))
     [:input {:type "checkbox" :checked false}]
     '(js/React.DOM.input (cljs.core/clj->js {:type "checkbox"}))))
  (testing "nil attributes"
    (are-html-expanded
     [:span {:class nil} "foo"] '(js/React.DOM.span (cljs.core/clj->js {}) "foo"))))

(deftest compiled-tags
  (testing "tag content can be vars"
    (let [x "foo"]
      (are-html-expanded
       [:span x] '(js/React.DOM.span (cljs.core/clj->js {}) "foo"))))
  (testing "tag content can be forms"
    (are-html-expanded
     [:span (str (+ 1 1))] '(js/React.DOM.span (cljs.core/clj->js {}) "2")
     [:span ({:foo "bar"} :foo)] '(js/React.DOM.span (cljs.core/clj->js {}) "bar")))
  (testing "attributes can contain vars"
    (let [id "id"]
      (are-html-expanded
       [:div {:id id}] '(js/React.DOM.div (cljs.core/clj->js {:id "id"}))
       [:div {id "id"}] '(js/React.DOM.div (cljs.core/clj->js {"id" "id"}))
       [:div {:id id} "bar"] '(js/React.DOM.div (cljs.core/clj->js {:id "id"}) "bar"))))
  (testing "attributes are evaluated"
    (are-html-expanded
     [:img {:src (str "/foo" "/bar")}] '(js/React.DOM.img (cljs.core/clj->js {:src "/foo/bar"}))
     [:div {:id (str "a" "b")} (str "foo")] '(js/React.DOM.div (cljs.core/clj->js {:id "ab"}) "foo")))
  (testing "type hints"
    (let [string "x"]
      (are-html-expanded
       [:span ^String string] '(js/React.DOM.span (cljs.core/clj->js {}) "x"))))
  (testing "optimized forms"
    (are-html-expanded
     ;; [:ul (for [n (range 3)] [:li n])]
     ;; "<ul><li>0</li><li>1</li><li>2</li></ul>"
     [:div (if true [:span "foo"] [:span "bar"])]
     '(js/React.DOM.div (cljs.core/clj->js {}) (js/React.DOM.span (cljs.core/clj->js {}) "foo"))))
  (testing "values are evaluated only once"
    (let [times-called (atom 0)
          foo #(swap! times-called inc)]
      (html-expand [:div (foo)])
      (is (= @times-called 1)))))
