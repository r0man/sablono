(ns react.who.core-test
  (:refer-clojure :exclude [replace])
  (:require-macros [cemerick.cljs.test :refer [are is deftest run-tests testing]]
                   [react.who.core :refer [html]]
                   [react.who.test :refer [are-html-rendered]])
  (:require [cemerick.cljs.test :as t]
            [clojure.string :refer [replace]]
            [goog.dom :as gdom]
            [react.who.core :as who]
            [react.who.util :refer [to-str]]
            [react.who.test :refer [render-dom]]))

(deftest tag-names
  (testing "basic tags"
    (are-html-rendered
     [:div] "<div></div>"
     ["div"] "<div></div>"
     ['div] "<div></div>"))
  (testing "tag syntax sugar"
    (are-html-rendered
     [:div#foo] "<div id=\"foo\"></div>"
     [:div.foo] "<div class=\"foo\"></div>"
     [:div.foo (str "bar" "baz")] "<div class=\"foo\">barbaz</div>"
     [:div.a.b] "<div class=\"a b\"></div>"
     [:div.a.b.c] "<div class=\"a b c\"></div>"
     [:div#foo.bar.baz] "<div id=\"foo\" class=\"bar baz\"></div>")))

(deftest tag-contents
  (testing "empty tags"
    (are-html-rendered
     [:div] "<div></div>"
     [:h1] "<h1></h1>"
     ;; [:script] "<script></script>"
     [:text] "<text></text>"
     [:a] "<a></a>"
     [:iframe] "<iframe></iframe>"
     [:title] "<title></title>"
     [:section] "<section></section>"))
  (testing "tags containing text"
    (are-html-rendered
     [:text "Lorem Ipsum"] "<text>Lorem Ipsum</text>"))
  (testing "contents are concatenated"
    (are-html-rendered
     [:div "foo" "bar"] "<div><span>foo</span><span>bar</span></div>"
     [:div [:p] [:br]] "<div><p></p><br></div>"))
  (testing "seqs are expanded"
    (are-html-rendered
     [:div (list "foo" "bar")] "<div><span>foo</span><span>bar</span></div>")
    ;; (is (= (html (list [:p "a"] [:p "b"])) "<p>a</p><p>b</p>"))
    )
  (testing "vecs don't expand - error if vec doesn't have tag name"
    (is (thrown? js/Error (html (vector [:p "a"] [:p "b"])))))
  (testing "tags can contain tags"
    (are-html-rendered
     [:div [:p]] "<div><p></p></div>"
     [:div [:b]] "<div><b></b></div>"
     [:p [:span [:a "foo"]]] "<p><span><a>foo</a></span></p>")))

(deftest tag-attributes
  (testing "tag with blank attribute map"
    (are-html-rendered
     [:div {}] "<div></div>"))
  (testing "tag with populated attribute map"
    (are-html-rendered
     [:div {:min "1", :max "2"}] "<div max=\"2\" min=\"1\"></div>"
     [:img {"id" "foo"}] "<img id=\"foo\">"
     [:img {:id "foo"}] "<img id=\"foo\">")
    ;; (is (= (html [:img {'id "foo"}]) "<img id=\"foo\">"))
    ;; (is (= (html [:div {:a "1", 'b "2", "c" "3"}])
    ;;        "<div a=\"1\" b=\"2\" c=\"3\" />"))
    )
  (testing "attribute values are escaped"
    (are-html-rendered
     [:div {:id "\""}] "<div id=\"&quot;\"></div>"))
  (testing "boolean attributes"
    (are-html-rendered
     [:input {:type "checkbox" :checked true}] "<input checked=\"true\" type=\"checkbox\">"
     [:input {:type "checkbox" :checked false}] "<input type=\"checkbox\">"))
  (testing "nil attributes"
    (are-html-rendered
     [:span {:class nil} "foo"] "<span>foo</span>")))

(deftest compiled-tags
  (testing "tag content can be vars"
    (let [x "foo"]
      (are-html-rendered
       [:span x] "<span>foo</span>")))
  (testing "tag content can be forms"
    (are-html-rendered
     [:span (str (+ 1 1))] "<span>2</span>"
     [:span ({:foo "bar"} :foo)] "<span>bar</span>"))
  (testing "attributes can contain vars"
    (let [id "id"]
      (are-html-rendered
       [:div {:id id}] "<div id=\"id\"></div>"
       [:div {id "id"}] "<div id=\"id\"></div>"
       [:div {:id id} "bar"] "<div id=\"id\">bar</div>")))
  (testing "attributes are evaluated"
    (are-html-rendered
     [:img {:src (str "/foo" "/bar")}] "<img src=\"/foo/bar\">"
     [:div {:id (str "a" "b")} (str "foo")] "<div id=\"ab\">foo</div>"))
  ;; (testing "type hints"
  ;;   (let [string "x"]
  ;;     (are-html-rendered
  ;;      [:span ^String string] "<span>x</span>")))
  (testing "optimized forms"
    (are-html-rendered
     [:ul (for [n (range 3)] [:li n])] "<ul><li>0</li><li>1</li><li>2</li></ul>")
    ;; (is (= (html [:div (if true
    ;;                      [:span "foo"]
    ;;                      [:span "bar"])])
    ;;        "<div><span>foo</span></div>"))
    )
  (testing "values are evaluated only once"
    (let [times-called (atom 0)
          foo #(swap! times-called inc)]
      (html [:div (foo)])
      (is (= @times-called 1)))))

;; (deftest render-modes
;;   (testing "closed tag"
;;     (is (= (html [:br]) "<br />"))
;;     (is (= (html {:mode :xml} [:br]) "<br />"))
;;     (is (= (html {:mode :sgml} [:br]) "<br>"))
;;     (is (= (html {:mode :html} [:br]) "<br>")))
;;   (testing "boolean attributes"
;;     (is (= (html {:mode :xml} [:input {:type "checkbox" :checked true}])
;;            "<input checked=\"checked\" type=\"checkbox\" />"))
;;     (is (= (html {:mode :sgml} [:input {:type "checkbox" :checked true}])
;;            "<input checked type=\"checkbox\">")))
;;   (testing "laziness and binding scope"
;;     (is (= (html {:mode :sgml} [:html [:link] (list [:link])])
;;            "<html><link><link></html>"))))

(deftest include-js-test
  (is (= (who/include-js "foo.js")
         (list [:script {:type "text/javascript", :src "foo.js"}])))
  (is (= (who/include-js "foo.js" "bar.js")
         (list [:script {:type "text/javascript", :src "foo.js"}]
               [:script {:type "text/javascript", :src "bar.js"}]))))

(deftest include-css-test
  (is (= (who/include-css "foo.css")
         (list [:link {:type "text/css", :href "foo.css", :rel "stylesheet"}])))
  (is (= (who/include-css "foo.css" "bar.css")
         (list [:link {:type "text/css", :href "foo.css", :rel "stylesheet"}]
               [:link {:type "text/css", :href "bar.css", :rel "stylesheet"}]))))

(deftest javascript-tag-test
  (is (= (who/javascript-tag "alert('hello');")
         [:script {:type "text/javascript"}
          "//<![CDATA[\nalert('hello');\n//]]>"])))

(deftest link-to-test
  (is (= (who/link-to "/")
         [:a {:href "/"} nil]))
  (is (= (who/link-to "/" "foo")
         [:a {:href "/"} (list "foo")]))
  (is (= (who/link-to "/" "foo" "bar")
         [:a {:href "/"} (list "foo" "bar")])))

(deftest mail-to-test
  (is (= (who/mail-to "foo@example.com")
         [:a {:href "mailto:foo@example.com"} "foo@example.com"]))
  (is (= (who/mail-to "foo@example.com" "foo")
         [:a {:href "mailto:foo@example.com"} "foo"])))

(deftest unordered-list-test
  (is (= (who/unordered-list ["foo" "bar" "baz"])
         [:ul (list [:li "foo"]
                    [:li "bar"]
                    [:li "baz"])])))

(deftest ordered-list-test
  (is (= (who/ordered-list ["foo" "bar" "baz"])
         [:ol (list [:li "foo"]
                    [:li "bar"]
                    [:li "baz"])])))

(comment (run-tests))
