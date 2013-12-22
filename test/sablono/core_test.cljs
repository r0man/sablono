(ns sablono.core-test
  (:refer-clojure :exclude [replace])
  (:require-macros [cemerick.cljs.test :refer [are is deftest run-tests testing]]
                   [sablono.core :refer [html with-group]]
                   [sablono.test :refer [are-html-rendered]])
  (:require [cemerick.cljs.test :as t]
            [clojure.string :refer [replace]]
            [goog.dom :as gdom]
            [sablono.core :as html]
            [sablono.util :refer [to-str]]
            [sablono.test :refer [render-dom]]))

(defn html-str [x]
  (render-dom (html x)))

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
  (is (= (html/include-js "foo.js")
         (list [:script {:type "text/javascript", :src "foo.js"}])))
  (is (= (html/include-js "foo.js" "bar.js")
         (list [:script {:type "text/javascript", :src "foo.js"}]
               [:script {:type "text/javascript", :src "bar.js"}]))))

(deftest include-css-test
  (is (= (html/include-css "foo.css")
         (list [:link {:type "text/css", :href "foo.css", :rel "stylesheet"}])))
  (is (= (html/include-css "foo.css" "bar.css")
         (list [:link {:type "text/css", :href "foo.css", :rel "stylesheet"}]
               [:link {:type "text/css", :href "bar.css", :rel "stylesheet"}]))))

(deftest javascript-tag-test
  (is (= (html/javascript-tag "alert('hello');")
         [:script {:type "text/javascript"}
          "//<![CDATA[\nalert('hello');\n//]]>"])))

(deftest link-to-test
  (is (= (html/link-to "/")
         [:a {:href "/"} nil]))
  (is (= (html/link-to "/" "foo")
         [:a {:href "/"} (list "foo")]))
  (is (= (html/link-to "/" "foo" "bar")
         [:a {:href "/"} (list "foo" "bar")])))

(deftest mail-to-test
  (is (= (html/mail-to "foo@example.com")
         [:a {:href "mailto:foo@example.com"} "foo@example.com"]))
  (is (= (html/mail-to "foo@example.com" "foo")
         [:a {:href "mailto:foo@example.com"} "foo"])))

(deftest unordered-list-test
  (is (= (html/unordered-list ["foo" "bar" "baz"])
         [:ul (list [:li "foo"]
                    [:li "bar"]
                    [:li "baz"])])))

(deftest ordered-list-test
  (is (= (html/ordered-list ["foo" "bar" "baz"])
         [:ol (list [:li "foo"]
                    [:li "bar"]
                    [:li "baz"])])))

(deftest test-hidden-field
  (is (= (html-str (html/hidden-field :foo "bar"))
         "<input id=\"foo\" type=\"hidden\" name=\"foo\" value=\"bar\">")))

(deftest test-hidden-field-with-extra-atts
  (is (= (html-str (html/hidden-field {:className "classy"} :foo "bar"))
         "<input id=\"foo\" class=\"classy\" type=\"hidden\" name=\"foo\" value=\"bar\">")))

(deftest test-text-field
  (is (= (html-str (html/text-field :foo))
         "<input id=\"foo\" type=\"text\" name=\"foo\">")))

(deftest test-text-field-with-extra-atts
  (is (= (html-str (html/text-field {:className "classy"} :foo "bar"))
         "<input id=\"foo\" class=\"classy\" type=\"text\" name=\"foo\" value=\"bar\">")))

(deftest test-check-box
  (is (= (html-str (html/check-box :foo true))
         "<input id=\"foo\" type=\"checkbox\" name=\"foo\" value=\"true\" checked=\"true\">")))

(deftest test-check-box-with-extra-atts
  (is (= (html-str (html/check-box {:className "classy"} :foo true 1))
         "<input id=\"foo\" class=\"classy\" type=\"checkbox\" name=\"foo\" value=\"1\" checked=\"true\">")))

(deftest test-password-field
  (is (= (html-str (html/password-field :foo "bar"))
         "<input id=\"foo\" type=\"password\" name=\"foo\" value=\"bar\">")))

(deftest test-password-field-with-extra-atts
  (is (= (html-str (html/password-field {:className "classy"} :foo "bar"))
         "<input id=\"foo\" class=\"classy\" type=\"password\" name=\"foo\" value=\"bar\">")))

(deftest test-email-field
  (is (= (html-str (html/email-field :foo "bar"))
         "<input id=\"foo\" type=\"email\" name=\"foo\" value=\"bar\">")))

(deftest test-email-field-with-extra-atts
  (is (= (html-str (html/email-field {:className "classy"} :foo "bar"))
         "<input id=\"foo\" class=\"classy\" type=\"email\" name=\"foo\" value=\"bar\">")))

(deftest test-radio-button
  (is (= (html-str (html/radio-button :foo true 1))
         "<input id=\"foo-1\" type=\"radio\" name=\"foo\" value=\"1\" checked=\"true\">")))

(deftest test-radio-button-with-extra-atts
  (is (= (html-str (html/radio-button {:className "classy"} :foo true 1))
         "<input id=\"foo-1\" class=\"classy\" type=\"radio\" name=\"foo\" value=\"1\" checked=\"true\">")))

;; (deftest test-select-options
;;   (are [x y] (= (html-str x) y)
;;        (select-options ["foo" "bar" "baz"])
;;        "<option>foo</option><option>bar</option><option>baz</option>"
;;        (select-options ["foo" "bar"] "bar")
;;        "<option>foo</option><option selected=\"selected\">bar</option>"
;;        (select-options [["Foo" 1] ["Bar" 2]])
;;        "<option value=\"1\">Foo</option><option value=\"2\">Bar</option>"
;;        (select-options [["Foo" [1 2]] ["Bar" [3 4]]])
;;        (str "<optgroup label=\"Foo\"><option>1</option><option>2</option></optgroup>"
;;             "<optgroup label=\"Bar\"><option>3</option><option>4</option></optgroup>")
;;        (select-options [["Foo" [["bar" 1] ["baz" 2]]]])
;;        (str "<optgroup label=\"Foo\"><option value=\"1\">bar</option>"
;;             "<option value=\"2\">baz</option></optgroup>")
;;        (select-options [["Foo" [1 2]]] 2)
;;        (str "<optgroup label=\"Foo\"><option>1</option>"
;;             "<option selected=\"selected\">2</option></optgroup>")))

;; (deftest test-drop-down
;;   (let [options ["op1" "op2"]
;;         selected "op1"
;;         select-options (html-str (select-options options selected))]
;;     (is (= (html-str (drop-down :foo options selected))
;;            (str "<select id=\"foo\" name=\"foo\">" select-options "</select>")))))

;; (deftest test-drop-down-with-extra-atts
;;   (let [options ["op1" "op2"]
;;         selected "op1"
;;         select-options (html-str (select-options options selected))]
;;     (is (= (html-str (drop-down {:className "classy"} :foo options selected))
;;            (str "<select class=\"classy\" id=\"foo\" name=\"foo\">"
;;                 select-options "</select>")))))

(deftest test-text-area
  (is (= (html-str (html/text-area :foo "bar"))
         "<textarea id=\"foo\" name=\"foo\" value=\"bar\">bar</textarea>")))

(deftest test-text-area-field-with-extra-atts
  (is (= (html-str (html/text-area {:className "classy"} :foo "bar"))
         "<textarea id=\"foo\" class=\"classy\" name=\"foo\" value=\"bar\">bar</textarea>")))

(deftest test-text-area-escapes
  (is (= (html-str (html/text-area :foo "bar</textarea>"))
         "<textarea id=\"foo\" name=\"foo\" value=\"bar&lt;/textarea&gt;\">bar&lt;/textarea&gt;</textarea>")))

(deftest test-file-field
  (is (= (html-str (html/file-upload :foo))
         "<input id=\"foo\" type=\"file\" name=\"foo\">")))

(deftest test-file-field-with-extra-atts
  (is (= (html-str (html/file-upload {:className "classy"} :foo))
         "<input id=\"foo\" class=\"classy\" type=\"file\" name=\"foo\">")))

(deftest test-label
  (is (= (html-str (html/label :foo "bar"))
         "<label for=\"foo\"><span>bar</span></label>")))

(deftest test-label-with-extra-atts
  (is (= (html-str (html/label {:className "classy"} :foo "bar"))
         "<label class=\"classy\" for=\"foo\"><span>bar</span></label>")))

(deftest test-submit
  (is (= (html-str (html/submit-button "bar"))
         "<input type=\"submit\" value=\"bar\">")))

(deftest test-submit-button-with-extra-atts
  (is (= (html-str (html/submit-button {:className "classy"} "bar"))
         "<input class=\"classy\" type=\"submit\" value=\"bar\">")))

(deftest test-reset-button
  (is (= (html-str (html/reset-button "bar"))
         "<input type=\"reset\" value=\"bar\">")))

(deftest test-reset-button-with-extra-atts
  (is (= (html-str (html/reset-button {:className "classy"} "bar"))
         "<input class=\"classy\" type=\"reset\" value=\"bar\">")))

(deftest test-form-to
  (is (= (html-str (html/form-to [:post "/path"] "foo" "bar"))
         "<form method=\"POST\" action=\"/path\"><span>foo</span><span>bar</span></form>")))

(deftest test-form-to-with-hidden-method
  (is (= (html-str (html/form-to [:put "/path"] "foo" "bar"))
         (str "<form method=\"POST\" action=\"/path\">"
              "<input id=\"_method\" type=\"hidden\" name=\"_method\" value=\"PUT\">"
              "<span>foo</span><span>bar</span></form>"))))

(deftest test-form-to-with-extr-atts
  (is (= (html-str (html/form-to {:className "classy"} [:post "/path"] "foo" "bar"))
         "<form class=\"classy\" method=\"POST\" action=\"/path\"><span>foo</span><span>bar</span></form>")))

(deftest test-with-group
  (testing "hidden-field"
    (is (= (html-str (with-group :foo (html/hidden-field :bar "val")))
           "<input id=\"foo-bar\" type=\"hidden\" name=\"foo[bar]\" value=\"val\">")))
  (testing "text-field"
    (is (= (html-str (with-group :foo (html/text-field :bar)))
           "<input id=\"foo-bar\" type=\"text\" name=\"foo[bar]\">")))
  (testing "checkbox"
    (is (= (html-str (with-group :foo (html/check-box :bar)))
           "<input id=\"foo-bar\" type=\"checkbox\" name=\"foo[bar]\" value=\"true\">")))
  (testing "password-field"
    (is (= (html-str (with-group :foo (html/password-field :bar)))
           "<input id=\"foo-bar\" type=\"password\" name=\"foo[bar]\">")))
  (testing "radio-button"
    (is (= (html-str (with-group :foo (html/radio-button :bar false "val")))
           "<input id=\"foo-bar-val\" type=\"radio\" name=\"foo[bar]\" value=\"val\">")))
  ;; (testing "drop-down"
  ;;   (is (= (html-str (with-group :foo (html/drop-down :bar [])))
  ;;          (str "<select id=\"foo-bar\" name=\"foo[bar]\"></select>"))))
  (testing "text-area"
    (is (= (html-str (with-group :foo (html/text-area :bar)))
           "<textarea id=\"foo-bar\" name=\"foo[bar]\" value=\"\"></textarea>")))
  (testing "file-upload"
    (is (= (html-str (with-group :foo (html/file-upload :bar)))
           "<input id=\"foo-bar\" type=\"file\" name=\"foo[bar]\">")))
  (testing "label"
    (is (= (html-str (with-group :foo (html/label :bar "Bar")))
           "<label for=\"foo-bar\"><span>Bar</span></label>")))
  (testing "multiple with-groups"
    (is (= (html-str (with-group :foo (with-group :bar (html/text-field :baz))))
           "<input id=\"foo-bar-baz\" type=\"text\" name=\"foo[bar][baz]\">")))
  (testing "multiple elements"
    (is (= (html-str (with-group :foo (html/label :bar "Bar") (html/text-field :var)))
           "<label for=\"foo-bar\"><span>Bar</span></label><input id=\"foo-var\" type=\"text\" name=\"foo[var]\">"))))

(comment (run-tests))
