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

(deftest test-render
  (are [markup match]
    (is (re-matches (re-pattern match) (html/render markup)))
    (html [:div#a.b "c"])
    "<div class=\"b\" id=\"a\" data-reactid=\".*\" data-react-checksum=\".*\">c</div>"
    (html [:div (when true [:p "data"]) (if true [:p "data"] nil)])
    "<div data-reactid=\".*\" data-react-checksum=\".*\"><p data-reactid=\".*\">data</p><p data-reactid=\".*\">data</p></div>"))

(deftest tag-names
  (testing "basic tags"
    (is (= (html-str [:div]) "<div></div>"))
    (is (= (html-str ["div"]) "<div></div>"))
    (is (= (html-str ['div]) "<div></div>")))
  (testing "tag syntax sugar"
    (is (= (html-str [:div#foo]) "<div id=\"foo\"></div>"))
    (is (= (html-str [:div.foo]) "<div class=\"foo\"></div>"))
    (is (= (html-str [:div.foo (str "bar" "baz")]) "<div class=\"foo\">barbaz</div>"))
    (is (= (html-str [:div.a.b]) "<div class=\"a b\"></div>"))
    (is (= (html-str [:div.a.b.c]) "<div class=\"a b c\"></div>"))
    (is (= (html-str [:div#foo.bar.baz]) "<div class=\"bar baz\" id=\"foo\"></div>"))
    (is (= (html-str [:div.jumbotron]) "<div class=\"jumbotron\"></div>"))))

(deftest tag-contents
  (testing "empty tags"
    (is (= (html-str [:div]) "<div></div>"))
    (is (= (html-str [:h1]) "<h1></h1>"))
    (is (= (html-str [:text]) "<text></text>"))
    (is (= (html-str [:a]) "<a></a>"))
    (is (= (html-str [:iframe]) "<iframe></iframe>"))
    (is (= (html-str [:title]) "<title></title>"))
    (is (= (html-str [:section]) "<section></section>")))
  (testing "tags containing text"
    (is (= (html-str [:text "Lorem Ipsum"]) "<text>Lorem Ipsum</text>")))
  (testing "contents are concatenated"
    (is (= (html-str [:div "foo" "bar"]) "<div><span>foo</span><span>bar</span></div>"))
    (is (= (html-str [:div [:p] [:br]]) "<div><p></p><br></div>")))
  (testing "seqs are expanded"
    (is (= (html-str [:div (list "foo" "bar")]) "<div><span>foo</span><span>bar</span></div>"))
    ;; (is (= (html (list [:p "a"] [:p "b"])) "<p>a</p><p>b</p>"))
    )
  (testing "vecs don't expand - error if vec doesn't have tag name"
    (is (thrown? js/Error (html (vector [:p "a"] [:p "b"])))))
  (testing "tags can contain tags"
    (is (= (html-str [:div [:p]]) "<div><p></p></div>"))
    (is (= (html-str [:div [:b]]) "<div><b></b></div>"))
    (is (= (html-str [:p [:span [:a "foo"]]]) "<p><span><a>foo</a></span></p>"))))

(deftest tag-attributes
  (testing "tag with blank attribute map"
    (is (= (html-str [:div {}]) "<div></div>")))
  (testing "tag with populated attribute map"
    (is (= (html-str [:div {:min "1", :max "2"}]) "<div min=\"1\" max=\"2\"></div>"))
    (is (= (html-str [:img {"id" "foo"}]) "<img id=\"foo\">"))
    (is (= (html-str [:img {:id "foo"}]) "<img id=\"foo\">"))
    ;; (is (= (html [:img {'id "foo"}]) "<img id=\"foo\">"))
    ;; (is (= (html [:div {:a "1", 'b "2", "c" "3"}])
    ;;        "<div a=\"1\" b=\"2\" c=\"3\" />"))
    )
  (testing "attribute values are escaped"
    (is (= (html-str [:div {:id "\""}]) "<div id=\"&quot;\"></div>")))
  (testing "boolean attributes"
    (is (= (html-str [:input {:type "checkbox" :checked true}]) "<input type=\"checkbox\" checked=\"\">"))
    (is (= (html-str [:input {:type "checkbox" :checked false}]) "<input type=\"checkbox\">")))
  (testing "nil attributes"
    (is (= (html-str [:span {:class nil} "foo"]) "<span>foo</span>")))
  (testing "interpreted attributes"
    (let [attr-fn (constantly {:id "a" :class "b" :http-equiv "refresh"})]
      (is (= (html-str [:span (attr-fn) "foo"])
             "<span id=\"a\" class=\"b\" httpequiv=\"refresh\">foo</span>"))))
  (testing "tag with aria attributes"
    (is (= (html-str [:div {:aria-disabled true}])
           "<div aria-disabled=\"true\"></div>")))
  (testing "tag with data attributes"
    (is (= (html-str [:div {:data-toggle "modal" :data-target "#modal"}])
           "<div data-toggle=\"modal\" data-target=\"#modal\"></div>"))))

(deftest compiled-tags
  (testing "tag content can be vars"
    (let [x "foo"]
      (is (= (html-str [:span x]) "<span>foo</span>"))))
  (testing "tag content can be forms"
    (is (= (html-str [:span (str (+ 1 1))]) "<span>2</span>"))
    (is (= (html-str [:span ({:foo "bar"} :foo)]) "<span>bar</span>")))
  (testing "attributes can contain vars"
    (let [id "id"]
      (is (= (html-str [:div {:id id}]) "<div id=\"id\"></div>"))
      (is (= (html-str [:div {id "id"}]) "<div id=\"id\"></div>"))
      (is (= (html-str [:div {:id id} "bar"]) "<div id=\"id\">bar</div>"))))
  (testing "attributes are evaluated"
    (is (= (html-str [:img {:src (str "/foo" "/bar")}]) "<img src=\"/foo/bar\">"))
    (is (= (html-str [:div {:id (str "a" "b")} (str "foo")]) "<div id=\"ab\">foo</div>")))
  ;; (testing "type hints"
  ;;   (let [string "x"]
  ;;     (are-html-rendered
  ;;      [:span ^String string] "<span>x</span>")))
  (testing "optimized forms"
    (is (= (html-str [:ul (for [n (range 3)] [:li n])]) "<ul><li>0</li><li>1</li><li>2</li></ul>"))
    (is (= (html-str [:div (if true
                             [:span "foo"]
                             [:span "bar"])])
           "<div><span>foo</span></div>")))
  (testing "values are evaluated only once"
    (let [times-called (atom 0)
          foo #(swap! times-called inc)]
      (html [:div (foo)])
      (is (= @times-called 1)))))

(deftest include-css-test
  (is (= (html/include-css "foo.css")
         '([:link {:type "text/css", :href "foo.css", :rel "stylesheet"}])))
  (is (= (html/include-css "foo.css" "bar.css")
         '([:link {:type "text/css", :href "foo.css", :rel "stylesheet"}]
             [:link {:type "text/css", :href "bar.css", :rel "stylesheet"}]))))

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
         "<input value=\"bar\" type=\"hidden\" name=\"foo\" id=\"foo\">")))

(deftest test-hidden-field-with-extra-atts
  (is (= (html-str (html/hidden-field {:class "classy"} :foo "bar"))
         "<input value=\"bar\" type=\"hidden\" name=\"foo\" id=\"foo\" class=\"classy\">")))

(deftest test-text-field
  (is (= (html-str (html/text-field :foo))
         "<input type=\"text\" name=\"foo\" id=\"foo\">"))
  (is (= (html-str (html/text-field :foo ""))
         "<input value=\"\" type=\"text\" name=\"foo\" id=\"foo\">"))
  (is (= (html-str (html/text-field :foo "bar"))
         "<input value=\"bar\" type=\"text\" name=\"foo\" id=\"foo\">")))

(deftest test-text-field-with-extra-atts
  (is (= (html-str (html/text-field {:class "classy"} :foo "bar"))
         "<input value=\"bar\" type=\"text\" name=\"foo\" id=\"foo\" class=\"classy\">")))

(deftest test-check-box
  (is (= (html-str (html/check-box :foo true))
         "<input value=\"true\" type=\"checkbox\" name=\"foo\" id=\"foo\" checked=\"\">")))

(deftest test-check-box-with-extra-atts
  (is (= (html-str (html/check-box {:class "classy"} :foo true 1))
         "<input value=\"1\" type=\"checkbox\" name=\"foo\" id=\"foo\" checked=\"\" class=\"classy\">")))

(deftest test-password-field
  (is (= (html-str (html/password-field :foo "bar"))
         "<input value=\"bar\" type=\"password\" name=\"foo\" id=\"foo\">")))

(deftest test-password-field-with-extra-atts
  (is (= (html-str (html/password-field {:class "classy"} :foo "bar"))
         "<input value=\"bar\" type=\"password\" name=\"foo\" id=\"foo\" class=\"classy\">")))

(deftest test-email-field
  (is (= (html-str (html/email-field :foo "bar"))
         "<input value=\"bar\" type=\"email\" name=\"foo\" id=\"foo\">")))

(deftest test-search-field
  (is (= (html-str (html/search-field :foo "bar"))
         "<input value=\"bar\" type=\"search\" name=\"foo\" id=\"foo\">")))

(deftest test-url-field
  (is (= (html-str (html/url-field :foo "bar"))
         "<input value=\"bar\" type=\"url\" name=\"foo\" id=\"foo\">")))

(deftest test-tel-field
  (is (= (html-str (html/tel-field :foo "bar"))
         "<input value=\"bar\" type=\"tel\" name=\"foo\" id=\"foo\">")))

(deftest test-number-field
  (is (= (html-str (html/number-field :foo "bar"))
         "<input value=\"bar\" type=\"number\" name=\"foo\" id=\"foo\">")))

(deftest test-range-field
  (is (= (html-str (html/range-field :foo "bar"))
         "<input value=\"bar\" type=\"range\" name=\"foo\" id=\"foo\">")))

(deftest test-date-field
  (is (= (html-str (html/date-field :foo "bar"))
         "<input value=\"bar\" type=\"date\" name=\"foo\" id=\"foo\">")))

(deftest test-month-field
  (is (= (html-str (html/month-field :foo "bar"))
         "<input value=\"bar\" type=\"month\" name=\"foo\" id=\"foo\">")))

(deftest test-week-field
  (is (= (html-str (html/week-field :foo "bar"))
         "<input value=\"bar\" type=\"week\" name=\"foo\" id=\"foo\">")))

(deftest test-time-field
  (is (= (html-str (html/time-field :foo "bar"))
         "<input value=\"bar\" type=\"time\" name=\"foo\" id=\"foo\">")))

(deftest test-datetime-field
  (is (= (html-str (html/datetime-field :foo "bar"))
         "<input value=\"bar\" type=\"datetime\" name=\"foo\" id=\"foo\">")))

(deftest test-datetime-local-field
  (is (= (html-str (html/datetime-local-field :foo "bar"))
         "<input value=\"bar\" type=\"datetime-local\" name=\"foo\" id=\"foo\">")))

(deftest test-color-field
  (is (= (html-str (html/color-field :foo "bar"))
         "<input value=\"bar\" type=\"color\" name=\"foo\" id=\"foo\">")))

(deftest test-email-field-with-extra-atts
  (is (= (html-str (html/email-field {:class "classy"} :foo "bar"))
         "<input value=\"bar\" type=\"email\" name=\"foo\" id=\"foo\" class=\"classy\">")))

(deftest test-radio-button
  (is (= (html-str (html/radio-button :foo true 1))
         "<input value=\"1\" type=\"radio\" name=\"foo\" id=\"foo-1\" checked=\"\">")))

(deftest test-radio-button-with-extra-atts
  (is (= (html-str (html/radio-button {:class "classy"} :foo true 1))
         "<input value=\"1\" type=\"radio\" name=\"foo\" id=\"foo-1\" checked=\"\" class=\"classy\">")))

(deftest test-select-options
  (are [x y] (= x y)
       (html-str (html/select-options ["foo" "bar" "baz"]))
       "<option>foo</option><option>bar</option><option>baz</option>"
       (html-str (html/select-options ["foo" "bar"] "bar"))
       "<option>foo</option><option selected=\"\">bar</option>"
       (html-str (html/select-options [["Foo" 1] ["Bar" 2]]))
       "<option value=\"1\">Foo</option><option value=\"2\">Bar</option>"
       ;; (html-str (html/select-options [["Foo" [1 2]] ["Bar" [3 4]]]))
       ;; (str "<optgroup label=\"Foo\"><option>1</option><option>2</option></optgroup>"
       ;;      "<optgroup label=\"Bar\"><option>3</option><option>4</option></optgroup>")
       ;; (html-str (html/select-options [["Foo" [["bar" 1] ["baz" 2]]]]))
       ;; (str "<optgroup label=\"Foo\"><option value=\"1\">bar</option>"
       ;;      "<option value=\"2\">baz</option></optgroup>")
       ;; (html-str (html/select-options [["Foo" [1 2]]] 2))
       ;; (str "<optgroup label=\"Foo\"><option>1</option>"
       ;;      "<option selected=\"true\">2</option></optgroup>")
       ))

(deftest test-drop-down
  (let [options ["op1" "op2"], selected "op1"
        select-options (html-str (html/select-options options selected))]
    (is (= (html-str (html/drop-down :foo options selected))
           (str "<select name=\"foo\" id=\"foo\">" select-options "</select>")))))

(deftest test-drop-down-with-extra-atts
  (let [options ["op1" "op2"], selected "op1"
        select-options (html-str (html/select-options options selected))]
    (is (= (html-str (html/drop-down {:class "classy"} :foo options selected))
           (str "<select name=\"foo\" id=\"foo\" class=\"classy\">"
                select-options "</select>")))))

(deftest test-text-area
  ;; TODO: There should be no value, right?
  (is (= (html-str (html/text-area :foo))
         "<textarea value=\"\" name=\"foo\" id=\"foo\"></textarea>"))
  (is (= (html-str (html/text-area :foo ""))
         "<textarea value=\"\" name=\"foo\" id=\"foo\"></textarea>"))
  (is (= (html-str (html/text-area :foo "bar"))
         "<textarea value=\"bar\" name=\"foo\" id=\"foo\">bar</textarea>")))

(deftest test-text-area-field-with-extra-atts
  (is (= (html-str (html/text-area {:class "classy"} :foo "bar"))
         "<textarea value=\"bar\" name=\"foo\" id=\"foo\" class=\"classy\">bar</textarea>")))

(deftest test-text-area-escapes
  (is (= (html-str (html/text-area :foo "bar</textarea>"))
         "<textarea value=\"bar&lt;/textarea&gt;\" name=\"foo\" id=\"foo\">bar&lt;/textarea&gt;</textarea>")))

(deftest test-file-field
  (is (= (html-str (html/file-upload :foo))
         "<input type=\"file\" name=\"foo\" id=\"foo\">")))

(deftest test-file-field-with-extra-atts
  (is (= (html-str (html/file-upload {:class "classy"} :foo))
         "<input type=\"file\" name=\"foo\" id=\"foo\" class=\"classy\">")))

(deftest test-label
  (is (= (html-str (html/label :foo "bar"))
         "<label for=\"foo\">bar</label>")))

(deftest test-label-with-extra-atts
  (is (= (html-str (html/label {:class "classy"} :foo "bar"))
         "<label for=\"foo\" class=\"classy\">bar</label>")))

(deftest test-submit
  (is (= (html-str (html/submit-button "bar"))
         "<input value=\"bar\" type=\"submit\">")))

(deftest test-submit-button-with-extra-atts
  (is (= (html-str (html/submit-button {:class "classy"} "bar"))
         "<input value=\"bar\" type=\"submit\" class=\"classy\">")))

(deftest test-reset-button
  (is (= (html-str (html/reset-button "bar"))
         "<input value=\"bar\" type=\"reset\">")))

(deftest test-reset-button-with-extra-atts
  (is (= (html-str (html/reset-button {:class "classy"} "bar"))
         "<input value=\"bar\" type=\"reset\" class=\"classy\">")))

(deftest test-form-to
  (is (= (html-str (html/form-to [:post "/path"] "foo" "bar"))
         "<form method=\"POST\" action=\"/path\"><span>foo</span><span>bar</span></form>")))

(deftest test-form-to-with-hidden-method
  (is (= (html-str (html/form-to [:put "/path"] "foo" "bar"))
         (str "<form method=\"POST\" action=\"/path\">"
              "<input value=\"PUT\" type=\"hidden\" name=\"_method\" id=\"_method\">"
              "<span>foo</span><span>bar</span></form>"))))

(deftest test-form-to-with-extr-atts
  (is (= (html-str (html/form-to {:class "classy"} [:post "/path"] "foo" "bar"))
         "<form method=\"POST\" action=\"/path\" class=\"classy\"><span>foo</span><span>bar</span></form>")))

(deftest test-with-group
  (testing "hidden-field"
    (is (= (html-str (with-group :foo (html/hidden-field :bar "val")))
           "<input value=\"val\" type=\"hidden\" name=\"foo[bar]\" id=\"foo-bar\">")))
  (testing "text-field"
    (is (= (html-str (with-group :foo (html/text-field :bar)))
           "<input type=\"text\" name=\"foo[bar]\" id=\"foo-bar\">")))
  (testing "checkbox"
    (is (= (html-str (with-group :foo (html/check-box :bar)))
           "<input value=\"true\" type=\"checkbox\" name=\"foo[bar]\" id=\"foo-bar\">")))
  (testing "password-field"
    (is (= (html-str (with-group :foo (html/password-field :bar)))
           "<input type=\"password\" name=\"foo[bar]\" id=\"foo-bar\">")))
  (testing "radio-button"
    (is (= (html-str (with-group :foo (html/radio-button :bar false "val")))
           "<input value=\"val\" type=\"radio\" name=\"foo[bar]\" id=\"foo-bar-val\">")))
  (testing "drop-down"
    (is (= (html-str (with-group :foo (html/drop-down :bar [])))
           (str "<select name=\"foo[bar]\" id=\"foo-bar\"></select>"))))
  (testing "text-area"
    (is (= (html-str (with-group :foo (html/text-area :bar "baz")))
           "<textarea value=\"baz\" name=\"foo[bar]\" id=\"foo-bar\">baz</textarea>")))
  (testing "file-upload"
    (is (= (html-str (with-group :foo (html/file-upload :bar)))
           "<input type=\"file\" name=\"foo[bar]\" id=\"foo-bar\">")))
  (testing "label"
    (is (= (html-str (with-group :foo (html/label :bar "Bar")))
           "<label for=\"foo-bar\">Bar</label>")))
  (testing "multiple with-groups"
    (is (= (html-str (with-group :foo (with-group :bar (html/text-field :baz))))
           "<input type=\"text\" name=\"foo[bar][baz]\" id=\"foo-bar-baz\">")))
  (testing "multiple elements"
    (is (= (html-str (with-group :foo (html/label :bar "Bar") (html/text-field :var)))
           "<label for=\"foo-bar\">Bar</label><input type=\"text\" name=\"foo[var]\" id=\"foo-var\">"))))

(deftest test-merge-attributes-let
  (let [classes (merge {:id "a"} {:class "b"})]
    (is (= "<div id=\"a\" class=\"b\">content</div>" (html-str [:div classes "content"])))))

(deftest test-issue-2-merge-class
  (is (= "<div class=\"a true\"></div>"
         (html-str [:div.a {:class (if (true? true) "true" "false")}])))
  (is (= "<div class=\"a b true\"></div>"
         (html-str [:div.a.b {:class (if (true? true) ["true"] "false")}]))))

(deftest test-issue-3-recursive-js-value
  (is (= "<div class=\"interaction-row\" style=\"position:relative;\"></div>"
         (html-str [:div.interaction-row {:style {:position "relative"}}])))
  (let [username "foo", hidden #(if %1 {:display "none"} {:display "block"})]
    (is (= (str "<ul class=\"nav navbar-nav navbar-right pull-right\">"
                "<li class=\"dropdown\" style=\"display:block;\">"
                "<a class=\"dropdown-toggle\" role=\"button\" href=\"#\"><span>Welcome, foo</span><span class=\"caret\"></span></a>"
                "<ul class=\"dropdown-menu\" role=\"menu\" style=\"left:0;\"></ul></li></ul>")
           (html-str [:ul.nav.navbar-nav.navbar-right.pull-right
                      [:li.dropdown {:style (hidden (nil? username))}
                       [:a.dropdown-toggle {:role "button" :href "#"} (str "Welcome, " username)
                        [:span.caret]]
                       [:ul.dropdown-menu {:role "menu" :style {:left 0}}]]])))))

(deftest test-issue-22-id-after-class
  (is (= "<div class=\"well\" id=\"setup\"></div>"
         (html-str [:div.well#setup]))))

(deftest test-issue-23-conditionals
  (are [form expected]
    (is (= expected form))
    (html-str (let [x true] (when x [:div]))) "<div></div>"
    (html-str (let [x false] (when x [:div]))) ""
    (html-str (let [x false] (when-not x [:div]))) "<div></div>"
    (html-str (let [x true] (when-not x [:div (str x)]))) ""
    (html-str (let [x true] (if-not x [:div]))) ""
    (html-str (let [x false] (if-not x [:div]))) "<div></div>"
    (let [x true] (html-str (if-not x [:div]))) ""
    (let [x false] (html-str (if-not x [:div]))) "<div></div>"
    (html-str [:div (if true {:class "test"})]) "<div class=\"test\"></div>"
    (html-str [:div (when true {:class "test"})]) "<div class=\"test\"></div>"
    (html-str [:div (if-not false {:class "test"})]) "<div class=\"test\"></div>"
    (html-str [:div (when-not false {:class "test"})]) "<div class=\"test\"></div>"))

(deftest test-issue-24-attr-and-keyword-classes
  (let [style-it (fn [p] {:placeholder (str p) :type "text"})]
    (is (= (html-str [:input.helloworld (style-it "dinosaurs")])
           "<input placeholder=\"dinosaurs\" type=\"text\" class=\"helloworld\">"))))

(deftest test-issue-25-comma-separated-class
  (is (= (html-str [:div.c1.c2 "text"])
         "<div class=\"c1 c2\">text</div>"))
  (is (= (html-str [:div.aa (merge {:class "bb"})])
         "<div class=\"aa bb\"></div>"))
  (is (= (let [input-classes ["large" "big"], autofocus true]
           (html-str [:input.form-control
                      (merge {:class input-classes})]))
         "<input class=\"form-control large big\">")))

(comment (run-tests))
