(ns sablono.core-test
  (:refer-clojure :exclude [replace])
  (:require-macros [cemerick.cljs.test :refer [are is deftest run-tests testing]]
                   [sablono.core :refer [html with-group]]
                   [sablono.test :refer [html-str]])
  (:require [cemerick.cljs.test :as t]
            [clojure.string :refer [replace]]
            [goog.dom :as gdom]
            [sablono.core :as html :include-macros true]
            [sablono.util :refer [to-str]]
            [sablono.test :refer [render-dom]]))

(deftest tag-names
  (testing "basic tags"
    (is (= (html-str [:div])) "<div></div>")
    (is (= (html-str ["div"])) "<div></div>")
    (is (= (html-str ['div])) "<div></div>"))
  (testing "tag syntax sugar"
    (is (= (html-str [:div#foo])) "<div id=\"foo\"></div>")
    (is (= (html-str [:div.foo])) "<div class=\"foo\"></div>")
    (is (= (html-str [:div.foo (str "bar" "baz")])) "<div class=\"foo\">barbaz</div>")
    (is (= (html-str [:div.a.b])) "<div class=\"a b\"></div>")
    (is (= (html-str [:div.a.b.c])) "<div class=\"a b c\"></div>")
    (is (= (html-str [:div#foo.bar.baz])) "<div id=\"foo\" class=\"bar baz\"></div>")))

(deftest tag-contents
  (testing "empty tags"
    (is (= (html-str [:div])) "<div></div>")
    (is (= (html-str [:h1])) "<h1></h1>")
    ;; [:script] "<script></script>"
    (is (= (html-str [:text])) "<text></text>")
    (is (= (html-str [:a])) "<a></a>")
    (is (= (html-str [:iframe])) "<iframe></iframe>")
    (is (= (html-str [:title])) "<title></title>")
    (is (= (html-str [:section])) "<section></section>"))
  (testing "tags containing text"
    (is (= (html-str [:text "Lorem Ipsum"])) "<text>Lorem Ipsum</text>"))
  (testing "contents are concatenated"
    (is (= (html-str [:div "foo" "bar"])) "<div><span>foo</span><span>bar</span></div>")
    (is (= (html-str [:div [:p] [:br]])) "<div><p></p><br></div>"))
  (testing "seqs are expanded"
    (is (= (html-str [:div (list "foo" "bar")])) "<div><span>foo</span><span>bar</span></div>")
    ;; (is (= (html (list [:p "a"] [:p "b"])) "<p>a</p><p>b</p>"))
    )
  (testing "vecs don't expand - error if vec doesn't have tag name"
    (is (thrown? js/Error (html (vector [:p "a"] [:p "b"])))))
  (testing "tags can contain tags"
    (is (= (html-str [:div [:p]])) "<div><p></p></div>")
    (is (= (html-str [:div [:b]])) "<div><b></b></div>")
    (is (= (html-str [:p [:span [:a "foo"]]])) "<p><span><a>foo</a></span></p>")))

(deftest tag-attributes
  (testing "tag with blank attribute map"
    (is (= (html-str [:div {}])) "<div></div>"))
  (testing "tag with populated attribute map"
    (is (= (html-str [:div {:min "1", :max "2"}])) "<div max=\"2\" min=\"1\"></div>")
    (is (= (html-str [:img {"id" "foo"}])) "<img id=\"foo\">")
    (is (= (html-str [:img {:id "foo"}])) "<img id=\"foo\">")
    ;; (is (= (html [:img {'id "foo"}]) "<img id=\"foo\">"))
    ;; (is (= (html [:div {:a "1", 'b "2", "c" "3"}])
    ;;        "<div a=\"1\" b=\"2\" c=\"3\" />"))
    )
  (testing "attribute values are escaped"
    (is (= (html-str [:div {:id "\""}])) "<div id=\"&quot;\"></div>"))
  (testing "boolean attributes"
    (is (= (html-str [:input {:type "checkbox" :checked true}])) "<input checked=\"true\" type=\"checkbox\">")
    (is (= (html-str [:input {:type "checkbox" :checked false}])) "<input type=\"checkbox\">"))
  (testing "nil attributes"
    (is (= (html-str [:span {:class nil} "foo"])) "<span>foo</span>")))

(deftest compiled-tags
  (testing "tag content can be vars"
    (let [x "foo"]
      (is (= (html-str [:span x])) "<span>foo</span>")))
  (testing "tag content can be forms"
    (is (= (html-str [:span (str (+ 1 1))])) "<span>2</span>")
    (is (= (html-str [:span ({:foo "bar"} :foo)])) "<span>bar</span>"))
  (testing "attributes can contain vars"
    (let [id "id"]
      (is (= (html-str [:div {:id id}])) "<div id=\"id\"></div>")
      (is (= (html-str [:div {id "id"}])) "<div id=\"id\"></div>")
      (is (= (html-str [:div {:id id} "bar"])) "<div id=\"id\">bar</div>")))
  (testing "attributes are evaluated"
    (is (= (html-str [:img {:src (str "/foo" "/bar")}])) "<img src=\"/foo/bar\">")
    (is (= (html-str [:div {:id (str "a" "b")} (str "foo")])) "<div id=\"ab\">foo</div>"))
  ;; (testing "type hints"
  ;;   (let [string "x"]
  ;;     (are-html-rendered
  ;;      [:span ^String string] "<span>x</span>")))
  (testing "optimized forms"
    (is (= (html-str [:ul (for [n (range 3)] [:li n])])) "<ul><li>0</li><li>1</li><li>2</li></ul>")
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

;; ;; (deftest render-modes
;; ;;   (testing "closed tag"
;; ;;     (is (= (html [:br]) "<br />"))
;; ;;     (is (= (html {:mode :xml} [:br]) "<br />"))
;; ;;     (is (= (html {:mode :sgml} [:br]) "<br>"))
;; ;;     (is (= (html {:mode :html} [:br]) "<br>")))
;; ;;   (testing "boolean attributes"
;; ;;     (is (= (html {:mode :xml} [:input {:type "checkbox" :checked true}])
;; ;;            "<input checked=\"checked\" type=\"checkbox\" />"))
;; ;;     (is (= (html {:mode :sgml} [:input {:type "checkbox" :checked true}])
;; ;;            "<input checked type=\"checkbox\">")))
;; ;;   (testing "laziness and binding scope"
;; ;;     (is (= (html {:mode :sgml} [:html [:link] (list [:link])])
;; ;;            "<html><link><link></html>"))))

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

(deftest test-issue-3-recursive-js-value
  (is (= "<div class=\"interaction-row\" style=\"position:relative;\"></div>"
         (html-str [:div.interaction-row {:style {:position "relative"}}]))))

(comment (run-tests))
