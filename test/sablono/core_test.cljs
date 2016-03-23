(ns sablono.core-test
  (:refer-clojure :exclude [replace])
  (:require-macros [sablono.core :refer [html with-group]]
                   [sablono.test :refer [html-str html-vec]])
  (:require [cljs.pprint :refer [pprint]]
            [cljs.test :as t :refer-macros [are is testing]]
            [clojure.string :refer [replace]]
            [devcards.core :refer-macros [defcard deftest]]
            [goog.dom :as gdom]
            [hickory.core :as hickory]
            [rum.core :as rum]
            [sablono.core :as html]
            [sablono.server :as server]
            [sablono.util :refer [to-str]]))

(deftest test-tag-names
  (testing "basic tags"
    (is (= (html-vec [:div]) [:div {}]))
    (is (= (html-vec ["div"]) [:div {}]))
    (is (= (html-vec ['div]) [:div {}])))
  (testing "tag syntax sugar"
    (is (= (html-vec [:div#foo]) [:div {:id "foo"}]))
    (is (= (html-vec [:div.foo]) [:div {:class "foo"}]))
    (is (= (html-vec [:div.foo (str "bar" "baz")])
           [:div {:class "foo"} "barbaz"]))
    (is (= (html-vec [:div.a.b])
           [:div {:class "a b"}]))
    (is (= (html-vec [:div.a.b.c])
           [:div {:class "a b c"}]))
    (is (= (html-vec [:div#foo.bar.baz])
           [:div {:class "bar baz" :id "foo"}]))
    (is (= (html-vec [:div.jumbotron])
           [:div {:class "jumbotron"}]))))

(deftest test-tag-contents
  (testing "empty tags"
    (is (= (html-vec [:div]) [:div {}]))
    (is (= (html-vec [:h1]) [:h1 {}]))
    (is (= (html-vec [:text]) [:text {}]))
    (is (= (html-vec [:a]) [:a {}]))
    (is (= (html-vec [:iframe]) [:iframe {}]))
    ;; TODO: Not properly parsed by hickory.
    ;; (is (= (html-vec [:title]) [:title {}]))
    (is (= (html-vec [:section]) [:section {}])))

  (testing "tags containing text"
    (is (= (html-vec [:text "Lorem Ipsum"])
           [:text {} "Lorem Ipsum" ])))
  (testing "contents are concatenated"
    (is (= (html-vec [:div "foo" "bar"])
           [:div {} "foobar"]))
    (is (= (html-vec [:div [:p] [:br]])
           [:div {} [:p {}] [:br {}]])))
  (testing "seqs are expanded"
    (is (= (html-vec [:div (list "foo" "bar")])
           [:div {} "foobar"])))
  (testing "tags can contain tags"
    (is (= (html-vec [:div [:p]])
           [:div {} [:p {}]]))
    (is (= (html-vec [:div [:b]])
           [:div {} [:b {}]]))
    (is (= (html-vec [:p [:span [:a "foo"]]])
           [:p {} [:span {} [:a {} "foo"]]]))))

(deftest test-tag-attributes
  (testing "tag with blank attribute map"
    (is (= (html-vec [:div {}])
           [:div {}])))
  (testing "tag with populated attribute map"
    (is (= (html-vec [:div {:min "1" :max "2"}])
           [:div {:min "1" :max "2"}]))
    (is (= (html-vec [:img {"id" "foo"}])
           [:img {:id "foo"}]))
    (is (= (html-vec [:img {:id "foo"}])
           [:img {:id "foo"}]))
    (is (= (html-vec [:img {'id "foo"}])
           [:img {:id "foo"}])))
  (testing "attribute values are escaped"
    (is (= (html-vec [:div {:id "\""}])
           [:div {:id "\""}])))
  (testing "boolean attributes"
    (is (= (html-vec [:input {:type "checkbox" :checked true}])
           [:input {:type "checkbox" :checked ""}]))
    (is (= (html-vec [:input {:type "checkbox" :checked false}])
           [:input {:type "checkbox"}])))
  (testing "nil attributes"
    (is (= (html-vec [:span {:class nil} "foo"])
           [:span {} "foo"])))
  (testing "interpreted attributes"
    (let [attr-fn (constantly {:id "a" :class "b" :http-equiv "refresh"})]
      (is (= (html-vec [:span (attr-fn) "foo"])
             [:span {:id "a" :http-equiv "refresh" :class "b"} "foo"]))))
  (testing "tag with aria attributes"
    (is (= (html-vec [:div {:aria-disabled true}])
           [:div {:aria-disabled "true"}])))
  (testing "tag with data attributes"
    (is (= (html-vec [:div {:data-toggle "modal" :data-target "#modal"}])
           [:div {:data-toggle "modal" :data-target "#modal"}]))))

(deftest test-compiled-tags
  (testing "tag content can be vars"
    (let [x "foo"]
      (is (= (html-vec [:span x])
             [:span {} "foo"]))))
  (testing "tag content can be forms"
    (is (= (html-vec [:span (str (+ 1 1))])
           [:span {} "2"]))
    (is (= (html-vec [:span ({:foo "bar"} :foo)])
           [:span {} "bar"])))
  (testing "attributes can contain vars"
    (let [id "id"]
      (is (= (html-vec [:div {:id id}])
             [:div {:id "id"}]))
      (is (= (html-vec [:div {id "id"}])
             [:div {:id "id"}]))
      (is (= (html-vec [:div {:id id} "bar"])
             [:div {:id "id"} "bar"]))))
  (testing "attributes are evaluated"
    (is (= (html-vec [:img {:src (str "/foo" "/bar")}])
           [:img {:src "/foo/bar"}]))
    (is (= (html-vec [:div {:id (str "a" "b")} (str "foo")])
           [:div {:id "ab"} "foo"])))
  (testing "optimized forms"
    (is (= (html-vec [:ul (for [n (range 3)] [:li {:key n} n])])
           [:ul {}
            [:li {} "0"]
            [:li {} "1"]
            [:li {} "2"]]))
    (is (= (html-vec [:div (if true [:span "foo"] [:span "bar"])])
           [:div {} [:span {} "foo"]])))
  (testing "values are evaluated only once"
    (let [times-called (atom 0)
          foo #(swap! times-called inc)]
      (html [:div (foo)])
      (is (= @times-called 1)))))

(deftest test-include-css
  (is (= (html/include-css "foo.css")
         [[:link {:type "text/css" :href "foo.css" :rel "stylesheet"}]]))
  (is (= (html/include-css "foo.css" "bar.css")
         [[:link {:type "text/css" :href "foo.css" :rel "stylesheet"}]
          [:link {:type "text/css" :href "bar.css" :rel "stylesheet"}]])))

(deftest test-link-to
  (is (= (html/link-to "/")
         [:a {:href "/"} nil]))
  (is (= (html/link-to "/" "foo")
         [:a {:href "/"} (list "foo")]))
  (is (= (html/link-to "/" "foo" "bar")
         [:a {:href "/"} (list "foo" "bar")])))

(deftest test-mail-to
  (is (= (html/mail-to "foo@example.com")
         [:a {:href "mailto:foo@example.com"} "foo@example.com"]))
  (is (= (html/mail-to "foo@example.com" "foo")
         [:a {:href "mailto:foo@example.com"} "foo"])))

(deftest test-unordered-list
  (is (= (html/unordered-list ["foo" "bar" "baz"])
         [:ul (list [:li "foo"]
                    [:li "bar"]
                    [:li "baz"])])))

(deftest test-ordered-list
  (is (= (html/ordered-list ["foo" "bar" "baz"])
         [:ol (list [:li "foo"]
                    [:li "bar"]
                    [:li "baz"])])))

(deftest test-input
  (is (= (html-vec [:input])
         [:input {}])))

(deftest test-input-with-extra-atts
  (is (= (html-vec [:input {:class "classy"}])
         [:input {:class "classy"}])))

(deftest test-hidden-field
  (is (= (html-vec (html/hidden-field :foo "bar"))
         [:input {:type "hidden" :name "foo" :id "foo" :value "bar"}])))

(deftest test-hidden-field-with-extra-atts
  (is (= (html-vec (html/hidden-field {:class "classy"} :foo "bar"))
         [:input {:type "hidden" :name "foo" :id "foo" :value "bar" :class "classy"}])))

(deftest test-text-field
  (is (= (html-vec (html/text-field :foo))
         [:input {:type "text" :name "foo" :id "foo"}]))
  (is (= (html-vec (html/text-field :foo ""))
         [:input {:type "text" :name "foo" :id "foo" :value ""}]))
  (is (= (html-vec (html/text-field :foo "bar"))
         [:input {:type "text" :name "foo" :id "foo" :value "bar"}])))

(deftest test-text-field-with-extra-atts
  (is (= (html-vec (html/text-field {:class "classy"} :foo "bar"))
         [:input {:type "text" :name "foo" :id "foo" :value "bar" :class "classy"}])))

(deftest test-check-box
  (is (= (html-vec (html/check-box :foo true))
         [:input {:type "checkbox" :name "foo" :id "foo" :value "true" :checked ""}])))

(deftest test-check-box-with-extra-atts
  (is (= (html-vec (html/check-box {:class "classy"} :foo true 1))
         [:input {:type "checkbox" :name "foo" :id "foo" :value "1" :checked "" :class "classy"}])))

(deftest test-password-field
  (is (= (html-vec (html/password-field :foo "bar"))
         [:input {:type "password" :name "foo" :id "foo" :value "bar"}])))

(deftest test-password-field-with-extra-atts
  (is (= (html-vec (html/password-field {:class "classy"} :foo "bar"))
         [:input {:type "password" :name "foo" :id "foo" :value "bar" :class "classy"}])))

(deftest test-email-field
  (is (= (html-vec (html/email-field :foo "bar"))
         [:input {:type "email" :name "foo" :id "foo" :value "bar"}])))

(deftest test-search-field
  (is (= (html-vec (html/search-field :foo "bar"))
         [:input {:type "search" :name "foo" :id "foo" :value "bar"}])))

(deftest test-url-field
  (is (= (html-vec (html/url-field :foo "bar"))
         [:input {:type "url" :name "foo" :id "foo" :value "bar"}])))

(deftest test-tel-field
  (is (= (html-vec (html/tel-field :foo "bar"))
         [:input {:type "tel" :name "foo" :id "foo" :value "bar"}])))

(deftest test-number-field
  (is (= (html-vec (html/number-field :foo "bar"))
         [:input {:type "number" :name "foo" :id "foo" :value "bar"}])))

(deftest test-range-field
  (is (= (html-vec (html/range-field :foo "bar"))
         [:input {:type "range" :name "foo" :id "foo" :value "bar"}])))

(deftest test-date-field
  (is (= (html-vec (html/date-field :foo "bar"))
         [:input {:type "date" :name "foo" :id "foo" :value "bar"}])))

(deftest test-month-field
  (is (= (html-vec (html/month-field :foo "bar"))
         [:input {:type "month" :name "foo" :id "foo" :value "bar"}])))

(deftest test-week-field
  (is (= (html-vec (html/week-field :foo "bar"))
         [:input {:type "week" :name "foo" :id "foo" :value "bar"}])))

(deftest test-time-field
  (is (= (html-vec (html/time-field :foo "bar"))
         [:input {:type "time" :name "foo" :id "foo" :value "bar"}])))

(deftest test-datetime-field
  (is (= (html-vec (html/datetime-field :foo "bar"))
         [:input {:type "datetime" :name "foo" :id "foo" :value "bar"}])))

(deftest test-datetime-local-field
  (is (= (html-vec (html/datetime-local-field :foo "bar"))
         [:input {:type "datetime-local" :name "foo" :id "foo" :value "bar"}])))

(deftest test-color-field
  (is (= (html-vec (html/color-field :foo "bar"))
         [:input {:type "color" :name "foo" :id "foo" :value "bar"}])))

(deftest test-email-field-with-extra-atts
  (is (= (html-vec (html/email-field {:class "classy"} :foo "bar"))
         [:input {:type "email" :name "foo" :id "foo" :value "bar" :class "classy"}])))

(deftest test-radio-button
  (is (= (html-vec (html/radio-button :foo true 1))
         [:input {:type "radio" :name "foo" :id "foo-1" :value "1" :checked ""}])))

(deftest test-radio-button-with-extra-atts
  (is (= (html-vec (html/radio-button {:class "classy"} :foo true 1))
         [:input {:type "radio" :name "foo" :id "foo-1" :value "1" :checked "" :class "classy"}])))

(deftest test-select-options
  (are [x y] (= x y)
    (html-vec [:select (html/select-options ["foo" "bar" "baz"])])
    [:select {}
     [:option {:value "foo"} "foo"]
     [:option {:value "bar"} "bar"]
     [:option {:value "baz"} "baz"]]
    (html-vec [:select (html/select-options [["Foo" 1] ["Bar" 2]])])
    [:select {}
     [:option {:value "1"} "Foo"]
     [:option {:value "2"} "Bar"]]
    (html-vec [:select (html/select-options [["Foo" 1 true] ["Bar" 2]])])
    [:select {}
     [:option {:value "1" :disabled ""} "Foo"]
     [:option {:value "2"} "Bar"]]
    (html-vec [:select (html/select-options [["Foo" [1 2]] ["Bar" [3 4]]])])
    [:select {}
     [:optgroup {:label "Foo"}
      [:option {:value "1"} "1"]
      [:option {:value "2"} "2"]]
     [:optgroup {:label "Bar"}
      [:option {:value "3"} "3"]
      [:option {:value "4"} "4"]]]
    (html-vec [:select (html/select-options [["Foo" [["bar" 1] ["baz" 2]]]])])
    [:select {}
     [:optgroup {:label "Foo"}
      [:option {:value "1"} "bar"]
      [:option {:value "2"} "baz"]]]))

(deftest test-drop-down
  (let [options ["op1" "op2"], selected "op1"
        select-options (html/select-options options)]
    (is (= (html-vec (html/drop-down :foo options selected))
           [:select {:name "foo" :id "foo"}
            [:option {:value "op1"} "op1"]
            [:option {:value "op2"} "op2"]]))))

(deftest test-drop-down-with-extra-atts
  (let [options ["op1" "op2"], selected "op1"
        select-options (html/select-options options)]
    (is (= (html-vec (html/drop-down {:class "classy"} :foo options selected))
           [:select {:name "foo" :id "foo" :class "classy"}
            [:option {:value "op1"} "op1"]
            [:option {:value "op2"} "op2"]]))))

(deftest test-text-area
  (is (= (html-vec (html/text-area :foo))
         [:textarea {:name "foo" :id "foo"}]))
  (is (= (html-vec (html/text-area :foo ""))
         [:textarea {:name "foo" :id "foo"}]))
  (is (= (html-vec (html/text-area :foo "bar"))
         [:textarea {:name "foo" :id "foo"} "bar"])))

(deftest test-text-area-field-with-extra-atts
  (is (= (html-vec (html/text-area {:class "classy"} :foo "bar"))
         [:textarea {:name "foo" :id "foo" :class "classy"} "bar"])))

(deftest test-text-area-escapes
  (is (= (html-vec (html/text-area :foo "bar</textarea>"))
         [:textarea {:name "foo" :id "foo"} "bar&lt;/textarea&gt;"])))

(deftest test-file-field
  (is (= (html-vec (html/file-upload :foo))
         [:input {:type "file" :name "foo" :id "foo"}])))

(deftest test-file-field-with-extra-atts
  (is (= (html-vec (html/file-upload {:class "classy"} :foo))
         [:input {:type "file" :name "foo" :id "foo" :class "classy"}])))

(deftest test-label
  (is (= (html-vec (html/label :foo "bar"))
         [:label {:for "foo"} "bar"])))

(deftest test-label-with-extra-atts
  (is (= (html-vec (html/label {:class "classy"} :foo "bar"))
         [:label {:for "foo" :class "classy"} "bar"])))

(deftest test-submit
  (is (= (html-vec (html/submit-button "bar"))
         [:input {:type "submit" :value "bar"}])))

(deftest test-submit-button-with-extra-atts
  (is (= (html-vec (html/submit-button {:class "classy"} "bar"))
         [:input {:type "submit" :value "bar" :class "classy"}])))

(deftest test-reset-button
  (is (= (html-vec (html/reset-button "bar"))
         [:input {:type "reset" :value "bar"}])))

(deftest test-reset-button-with-extra-atts
  (is (= (html-vec (html/reset-button {:class "classy"} "bar"))
         [:input {:type "reset" :value "bar" :class "classy"}])))

(deftest test-form-to
  (is (= (html-vec (html/form-to [:post "/path"] "foo" "bar"))
         [:form {:method "POST" :action "/path"} "foobar"])))

(deftest test-form-to-with-hidden-method
  (is (= (html-vec (html/form-to [:put "/path"] "foo" "bar"))
         [:form {:method "POST" :action "/path"}
          [:input {:type "hidden" :name "_method" :id "_method" :value "PUT"}]
          "foobar"])))

(deftest test-form-to-with-extr-atts
  (is (= (html-vec (html/form-to {:class "classy"} [:post "/path"] "foo" "bar"))
         [:form {:method "POST" :action "/path" :class "classy"} "foobar"])))

(deftest test-with-group
  (testing "hidden-field"
    (is (= (html-vec [:form (with-group :foo (html/hidden-field {:key 0} :bar "val"))])
           [:form {} [:input {:type "hidden" :name "foo[bar]" :id "foo-bar" :value "val"}]])))
  (testing "text-field"
    (is (= (html-vec [:form (with-group :foo (html/text-field {:key 0} :bar))])
           [:form {} [:input {:type "text" :name "foo[bar]" :id "foo-bar"}]])))
  (testing "checkbox"
    (is (= (html-vec [:form (with-group :foo (html/check-box {:key 0} :bar))])
           [:form {} [:input {:type "checkbox" :name "foo[bar]" :id "foo-bar" :value "true"}]])))
  (testing "password-field"
    (is (= (html-vec [:form (with-group :foo (html/password-field {:key 0} :bar))])
           [:form {} [:input {:type "password" :name "foo[bar]" :id "foo-bar"}]])))
  (testing "radio-button"
    (is (= (html-vec [:form (with-group :foo (html/radio-button {:key 0} :bar false "val"))])
           [:form {} [:input {:type "radio" :name "foo[bar]" :id "foo-bar-val" :value "val"}]])))
  (testing "drop-down"
    (is (= (html-vec [:form (with-group :foo (html/drop-down {:key 0} :bar []))])
           [:form {} [:select {:name "foo[bar]" :id "foo-bar"}]])))
  (testing "text-area"
    (is (= (html-vec [:form (with-group :foo (html/text-area {:key 0} :bar "baz"))])
           [:form {} [:textarea {:name "foo[bar]" :id "foo-bar"} "baz"]])))
  (testing "file-upload"
    (is (= (html-vec [:form (with-group :foo (html/file-upload {:key 0} :bar))])
           [:form {} [:input {:type "file" :name "foo[bar]" :id "foo-bar"}]])))
  (testing "label"
    (is (= (html-vec [:form (with-group :foo (html/label {:key 0} :bar "Bar"))])
           [:form {} [:label {:for "foo-bar"} "Bar"]])))
  (testing "multiple with-groups"
    (is (= (html-vec [:form (with-group :foo (with-group :bar (html/text-field {:key 0} :baz)))])
           [:form {} [:input {:type "text" :name "foo[bar][baz]" :id "foo-bar-baz"}]])))
  (testing "multiple elements"
    (is (= (html-vec [:form (with-group :foo
                              (html/label {:key 0} :bar "Bar")
                              (html/text-field {:key 1} :var))])
           [:form {}
            [:label {:for "foo-bar"} "Bar"]
            [:input {:type "text" :name "foo[var]" :id "foo-var"}]]))))

(deftest test-merge-attributes-let
  (let [classes (merge {:id "a"} {:class "b"})]
    (is (= (html-vec [:div classes "content"])
           [:div {:id "a" :class "b"} "content"]))))

(deftest test-issue-2-merge-class
  (is (= (html-vec [:div.a {:class (if (true? true) "true" "false")}])
         [:div {:class "a true"}]))
  (is (= (html-vec [:div.a.b {:class (if (true? true) ["true"] "false")}])
         [:div {:class "a b true"}])))

(deftest test-issue-3-recursive-js-value
  (is (= (html-vec [:div.interaction-row {:style {:position "relative"}}])
         [:div {:class "interaction-row" :style "position:relative;"}]))
  (let [username "foo"
        hidden #(if %1 {:display "none"} {:display "block"})]
    (is (= (html-vec [:ul.nav.navbar-nav.navbar-right.pull-right
                      [:li.dropdown {:style (hidden (nil? username))}
                       [:a.dropdown-toggle {:role "button" :href "#"} (str "Welcome, " username)
                        [:span.caret]]
                       [:ul.dropdown-menu {:role "menu" :style {:left 0}}]]])
           [:ul {:class "nav navbar-nav navbar-right pull-right"}
            [:li {:class "dropdown" :style "display:block;"}
             [:a {:class "dropdown-toggle" :role "button" :href "#"}
              "Welcome, foo" [:span {:class "caret"}]]
             [:ul {:class "dropdown-menu" :role "menu" :style "left:0;"}]]]))))

(deftest test-issue-22-id-after-class
  (is (= (html-vec [:div.well#setup])
         [:div {:class "well" :id "setup"}])))

(deftest test-issue-23-conditionals
  (are [form expected]
      (= expected form)
    (html-vec (let [x true] (when x [:div])))
    [:div {}]
    (html-vec (let [x false] (when x [:div])))
    nil
    (html-vec (let [x false] (when-not x [:div])))
    [:div {}]
    (html-vec (let [x true] (when-not x [:div (str x)])))
    nil
    (html-vec (let [x true] (if-not x [:div])))
    nil
    (html-vec (let [x false] (if-not x [:div])))
    [:div {}]
    (let [x true] (html-vec (if-not x [:div])))
    nil
    (let [x false] (html-vec (if-not x [:div])))
    [:div {}]
    (html-vec [:div (if true {:class "test"})])
    [:div {:class "test"}]
    (html-vec [:div (when true {:class "test"})])
    [:div {:class "test"}]
    (html-vec [:div (if-not false {:class "test"})])
    [:div {:class "test"}]
    (html-vec [:div (when-not false {:class "test"})])
    [:div {:class "test"}]
    (let [x 1] (html-vec (when x [:div x])))
    [:div {} "1"]
    (let [x 1] (html-vec (when-not x [:div x])))
    nil))

(deftest test-issue-24-attr-and-keyword-classes
  (let [style-it (fn [p] {:placeholder (str p) :type "text"})]
    (is (= (html-vec [:input.helloworld (style-it "dinosaurs")])
           [:input {:placeholder "dinosaurs" :type "text" :class "helloworld"}]))))

(deftest test-issue-25-comma-separated-class
  (is (= (html-vec [:div.c1.c2 "text"])
         [:div {:class "c1 c2"} "text"]))
  (is (= (html-vec [:div.aa (merge {:class "bb"})])
         [:div {:class "aa bb"}]))
  (is (= (let [input-classes ["large" "big"]]
           (html-vec [:input.form-control
                      (merge {:class input-classes})]))
         [:input {:class "form-control large big"}])))

(deftest test-issue-33-number-warning
  (is (= (html-vec [:div (count [1 2 3])])
         [:div {} "3"])))

(deftest test-issue-37-camel-case-style-attrs
  (is (= (html-vec [:div {:style {:z-index 1000}}])
         [:div {:style "z-index:1000;"}]))
  (is (= (html-vec [:div (merge {:style {:z-index 1000}})])
         [:div {:style "z-index:1000;"}])))

(deftest test-div-with-nested-lazy-seq
  (is (= (html-vec [:div (map identity ["A" "B"])])
         [:div {} "AB"])))

(deftest test-div-with-nested-list
  (is (= (html-vec [:div (list "A" "B")])
         [:div {} "AB"])))

(deftest test-div-with-nested-vector
  (is (= (html-vec [:div ["A" "B"]])
         [:div {} "AB"]))
  (is (= (html-vec [:div (vector"A" "B")])
         [:div {} "AB"])))

(deftest test-class-duplication
  (is (= (html-vec [:div.a.a.b.b.c {:class "c"}])
         [:div {:class "a a b b c c"}]))  )

(deftest test-class-order
  (is (= (html-vec [:div.a.b.c {:class "d"}])
         [:div {:class "a b c d"}]))
  (is (= (html-vec [:div.a.b.c {:class ["foo" "bar"]}])
         [:div {:class "a b c foo bar"}])))

(deftest test-class-as-set
  (is (= (html-vec [:div {:class #{"a" "b" "c"}}])
         [:div {:class "a b c"}]))
  (is (= (html-vec [:div {:class (set ["a" "b" "c"])}])
         [:div {:class "a b c"}])))

(deftest test-class-as-list
  (is (= (html-vec [:div {:class '("a" "b" "c")}])
         [:div {:class "a b c"}]))
  (is (= (html-vec [:div {:class (list "a" "b" "c")}])
         [:div {:class "a b c"}])))

(deftest test-class-as-vector
  (is (= (html-vec [:div {:class ["a" "b" "c"]}])
         [:div {:class "a b c"}]))
  (is (= (html-vec [:div {:class (vector "a" "b" "c")}])
         [:div {:class "a b c"}])))

(deftest test-issue-80
  (is (= (html-vec
          [:div
           [:div {:class (list "foo" "bar")}]
           [:div {:class (vector "foo" "bar")}]
           (let []
             [:div {:class (list "foo" "bar")}])
           (let []
             [:div {:class (vector "foo" "bar")}])
           (when true
             [:div {:class (list "foo" "bar")}])
           (when true
             [:div {:class (vector "foo" "bar")}])
           (do
             [:div {:class (list "foo" "bar")}])
           (do
             [:div {:class (vector "foo" "bar")}])])
         [:div {}
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]
          [:div {:class "foo bar"}]])))

(deftest test-issue-90
  (is (= (html-vec [:div nil (case :a :a "a")])
         [:div {} "a"])))

(deftest test-complex-scenario
  (is (= (html-vec [:div.a {:class (list "b")} (case :a :a "a")])
         [:div {:class "a b"} "a"])))

(deftest test-issue-57
  (let [payload {:username "john" :likes 2}]
    (is (= (html-vec
            (let [{:keys [username likes]} payload]
              [:div
               [:div (str username " (" likes ")")]
               [:div "!Pixel Scout"]]))
           [:div {}
            [:div {} "john (2)"]
            [:div {} "!Pixel Scout"]]))))

(rum/defc issue-57-rum [text]
  (html
   (let [text-add (str text " warning")]
     [:div
      [:h1 text]
      [:h1 text-add]])))

(deftest test-issue-57-rum
  (is (= (html-vec (issue-57-rum "This gives"))
         [:div
          {}
          [:h1 {} "This gives"]
          [:h1 {} "This gives warning"]])))

(deftest test-issue-115
  (is (= (html-vec [:a {:id :XY}])
         [:a {:id "XY"}]))
  (is (= (html-vec [:a (identity {:id :XY})])
         [:a {:id "XY"}])))
