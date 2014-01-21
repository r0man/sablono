(ns sablono.compiler-test
  (:require [clojure.test :refer :all]
            [clojure.walk :refer [prewalk]]
            [sablono.core :refer [html html-expand]]
            [sablono.compiler :refer :all])
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

(defn wrap-js-value [forms]
  (prewalk
   (fn [form]
     (if (instance? JSValue form)
       (JSValueWrapper. (wrap-js-value (.val form))) form))
   forms))

(defn replace-gensyms [forms]
  (prewalk
   (fn [form]
     (if (and (symbol? form)
              (re-matches #"attrs\d+" (str form)))
       'attrs form))
   forms))

(defmacro are-html-expanded [& body]
  `(are [form# expected#]
     (is (= (wrap-js-value expected#)
            (wrap-js-value (replace-gensyms (html-expand form#)))))
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
  (is (= (wrap-js-value
          '(into-array [(js/React.DOM.div #js {:id "a"})
                        (js/React.DOM.div #js {:id "b"})]))
         (wrap-js-value (html-expand [:div#a] [:div#b])))))

(deftest tag-names
  (testing "basic tags"
    (are-html-expanded
     '[:div] '(js/React.DOM.div nil)
     '[:div] '(js/React.DOM.div nil)
     '["div"] '(js/React.DOM.div nil)
     '['div] '(js/React.DOM.div nil)))
  (testing "tag syntax sugar"
    (are-html-expanded
     '[:div#foo] '(js/React.DOM.div #js {:id "foo"})
     '[:div.foo] '(js/React.DOM.div #js {:className "foo"})
     '[:div.foo (str "bar" "baz")]
     '(let* [attrs (str "bar" "baz")]
            (if (clojure.core/map? attrs)
              (js/React.DOM.div (sablono.interpreter/attributes (sablono.util/merge-with-class {:class ["foo"]} attrs)) nil)
              (js/React.DOM.div #js {:className "foo"} (sablono.interpreter/interpret attrs))))
     '[:div.a.b] '(js/React.DOM.div #js {:className "a b"})
     '[:div.a.b.c] '(js/React.DOM.div #js {:className "a b c"})
     '[:div#foo.bar.baz] '(js/React.DOM.div #js {:id "foo", :className "bar baz"}))))

(deftest tag-contents
  (testing "empty tags"
    (are-html-expanded
     '[:div] '(js/React.DOM.div nil)
     '[:h1] '(js/React.DOM.h1 nil)
     '[:script] '(js/React.DOM.script nil)
     '[:text] '(js/React.DOM.text nil)
     '[:a] '(js/React.DOM.a nil)
     '[:iframe] '(js/React.DOM.iframe nil)
     '[:title] '(js/React.DOM.title nil)
     '[:section] '(js/React.DOM.section nil)))
  (testing "tags containing text"
    (are-html-expanded
     '[:text "Lorem Ipsum"] '(js/React.DOM.text nil "Lorem Ipsum")))
  (testing "contents are concatenated"
    (are-html-expanded
     '[:div "foo" "bar"]
     '(js/React.DOM.div nil "foo" "bar")
     '[:div [:p] [:br]]
     '(js/React.DOM.div
       nil
       (js/React.DOM.p nil)
       (js/React.DOM.br nil))))
  (testing "seqs are expanded"
    (are-html-expanded
     '[:div (list "foo" "bar")]
     '(let* [attrs (list "foo" "bar")]
            (if (clojure.core/map? attrs)
              (js/React.DOM.div (sablono.interpreter/attributes attrs) nil)
              (js/React.DOM.div nil (sablono.interpreter/interpret attrs))))
     '(list [:p "a"] [:p "b"])
     '(sablono.interpreter/interpret (list [:p "a"] [:p "b"]))))
  (testing "vecs don't expand - error if vec doesn't have tag name"
    (is (thrown? Exception (html (vector [:p "a"] [:p "b"])))))
  (testing "tags can contain tags"
    (are-html-expanded
     '[:div [:p]]
     '(js/React.DOM.div nil (js/React.DOM.p nil))
     '[:div [:b]]
     '(js/React.DOM.div nil (js/React.DOM.b nil))
     '[:p [:span [:a "foo"]]]
     '(js/React.DOM.p nil (js/React.DOM.span nil (js/React.DOM.a nil "foo"))))))

(deftest tag-attributes
  (testing "tag with blank attribute map"
    (are-html-expanded
     '[:div {}] '(js/React.DOM.div nil)))
  (testing "tag with populated attribute map"
    (are-html-expanded
     '[:div {:min "1", :max "2"}] '(js/React.DOM.div #js {:min "1", :max "2"})
     '[:img {"id" "foo"}] '(js/React.DOM.img #js {"id" "foo"})
     '[:img {:id "foo"}] '(js/React.DOM.img #js {:id "foo"})))
  (testing "attribute values are escaped"
    (are-html-expanded
     '[:div {:id "\""}] '(js/React.DOM.div #js {:id "\""})))
  (testing "attributes are converted to their DOM equivalents"
    (are-html-expanded
     '[:div {:class "classy"}] '(js/React.DOM.div #js {:className "classy"})
     '[:div {:data-foo-bar "baz"}] '(js/React.DOM.div #js {:data-foo-bar "baz"})
     '[:div {:content-editable "baz"}] '(js/React.DOM.div #js {:contentEditable "baz"})
     '[:label {:for "foo"}] '(js/React.DOM.label #js {:htmlFor "foo"})))
  (testing "boolean attributes"
    (are-html-expanded
     '[:input {:type "checkbox" :checked true}]
     '(js/React.DOM.input #js {:checked true, :type "checkbox"})
     '[:input {:type "checkbox" :checked false}]
     '(js/React.DOM.input #js {:type "checkbox"})))
  (testing "nil attributes"
    (are-html-expanded
     '[:span {:class nil} "foo"] '(js/React.DOM.span nil "foo")))
  (testing "empty attributes"
    (are-html-expanded
     '[:span {} "foo"] '(js/React.DOM.span nil "foo"))))

(deftest compiled-tags
  (testing "tag content can be vars"
    (let [x "foo"]
      (are-html-expanded
       '[:span x]
       '(let* [attrs x]
              (if (clojure.core/map? attrs)
                (js/React.DOM.span (sablono.interpreter/attributes attrs) nil)
                (js/React.DOM.span nil (sablono.interpreter/interpret attrs)))))))
  (testing "tag content can be forms"
    (are-html-expanded
     '[:span (str (+ 1 1))]
     '(let* [attrs (str (+ 1 1))]
            (if (clojure.core/map? attrs)
              (js/React.DOM.span (sablono.interpreter/attributes attrs) nil)
              (js/React.DOM.span nil (sablono.interpreter/interpret attrs))))
     [:span ({:foo "bar"} :foo)] '(js/React.DOM.span nil "bar")))
  (testing "attributes can contain vars"
    (let [id "id"]
      (are-html-expanded
       '[:div {:id id}] '(js/React.DOM.div #js {:id id})
       '[:div {:id id} "bar"] '(js/React.DOM.div #js {:id id} "bar"))))
  (testing "attributes are evaluated"
    (are-html-expanded
     '[:img {:src (str "/foo" "/bar")}]
     '(js/React.DOM.img #js {:src (str "/foo" "/bar")})
     '[:div {:id (str "a" "b")} (str "foo")]
     '(js/React.DOM.div #js {:id (str "a" "b")} (sablono.interpreter/interpret (str "foo")))))
  (testing "type hints"
    (let [string "x"]
      (are-html-expanded
       '[:span ^String string] '(js/React.DOM.span nil string))))
  (testing "optimized forms"
    (are-html-expanded
     '[:ul (for [n (range 3)] [:li n])]
     '(js/React.DOM.ul
       nil
       (into-array (clojure.core/for [n (range 3)]
                     (clojure.core/let [attrs n]
                       (if (clojure.core/map? attrs)
                         (js/React.DOM.li
                          (sablono.interpreter/attributes attrs) nil)
                         (js/React.DOM.li nil (sablono.interpreter/interpret attrs)))))))
     '[:div (if true [:span "foo"] [:span "bar"])]
     '(let* [attrs (if true [:span "foo"] [:span "bar"])]
            (if (clojure.core/map? attrs)
              (js/React.DOM.div (sablono.interpreter/attributes attrs) nil)
              (js/React.DOM.div nil (sablono.interpreter/interpret attrs))))))
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
   '(js/React.DOM.li
     nil
     (js/React.DOM.a
      #js {:href (str "#show/" (:key datum))})
     (js/React.DOM.div
      #js {:id (str "item" (:key datum)), :className "class1 class2"}
      (js/React.DOM.span #js {:className "anchor"} (sablono.interpreter/interpret (:name datum)))))))

(deftest test-issue-2-merge-class
  (are-html-expanded
   '[:div.a {:class (if (true? true) "true" "false")}]
   '(js/React.DOM.div #js {:className (sablono.util/join-classes ["a" (if (true? true) "true" "false")])})
   '[:div.a.b {:class (if (true? true) ["true"] "false")}]
   '(js/React.DOM.div #js {:className (sablono.util/join-classes ["a" "b" (if (true? true) ["true"] "false")])})))

(deftest test-issue-3-recursive-js-literal
  (are-html-expanded
   '[:div.interaction-row {:style {:position "relative"}}]
   '(js/React.DOM.div #js {:className "interaction-row", :style #js {:position "relative"}}))
  (let [username "foo", hidden #(if %1 {:display "none"} {:display "block"})]
    (are-html-expanded
     '[:ul.nav.navbar-nav.navbar-right.pull-right
       [:li.dropdown {:style (hidden (nil? username))}
        [:a.dropdown-toggle {:role "button" :href "#"} (str "Welcome, " username)
         [:span.caret]]
        [:ul.dropdown-menu {:role "menu" :style {:left 0}}]]]
     '(js/React.DOM.ul
       #js {:className "nav navbar-nav navbar-right pull-right"}
       (js/React.DOM.li
        #js {:style (clj->js (hidden (nil? username))), :className "dropdown"}
        (js/React.DOM.a
         #js {:href "#", :role "button", :className "dropdown-toggle"}
         (sablono.interpreter/interpret (str "Welcome, " username))
         (js/React.DOM.span
          #js {:className "caret"}))
        (js/React.DOM.ul
         #js {:role "menu", :style #js {:left 0}, :className "dropdown-menu"}))))))
