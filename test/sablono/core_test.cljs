(ns sablono.core-test
  (:require-macros [sablono.core :refer [html with-group]]
                   [sablono.test :refer [html-data]])
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test :refer [are is testing]]
            [devcards.core :refer-macros [defcard deftest]]
            [rum.core :as rum]
            [sablono.core :as html]
            [sablono.server :as server]
            [sablono.util :refer [to-str]]
            [tubax.core :refer [xml->clj]]))

(deftest test-basic-tag
  (is (= (html-data [:div])
         {:tag :div
          :attributes {}
          :content []}))
  ;; TODO: Deprecate string
  (is (= (html-data ["div"])
         {:tag :div
          :attributes {}
          :content []}))
  ;; TODO: Deprecate symbol
  (is (= (html-data ['div])
         {:tag :div
          :attributes {}
          :content []})))

(deftest test-tag-syntax-sugar-id
  (is (= (html-data [:div#foo])
         {:tag :div
          :attributes {:id "foo"}
          :content []})))

(deftest test-tag-syntax-sugar-class
  (is (= (html-data [:div.foo])
         {:tag :div
          :attributes {:class "foo"}
          :content []})))

(deftest test-tag-syntax-sugar-class-content
  (is (= (html-data [:div.foo (str "bar" "baz")])
         {:tag :div
          :attributes {:class "foo"}
          :content ["barbaz"]})))

(deftest test-tag-syntax-sugar-multiple-classes
  (is (= (html-data [:div.a.b.c])
         {:tag :div
          :attributes {:class "a b c"}
          :content []})))

(deftest test-tag-syntax-sugar-class-id
  (is (= (html-data [:div#foo.bar.baz])
         {:tag :div
          :attributes {:id "foo" :class "bar baz"}
          :content []})))

(deftest test-tag-content-empty
  (is (= (html-data [:div])
         {:tag :div
          :attributes {}
          :content []})))

(deftest test-tag-content-text
  (is (= (html-data [:text "Lorem Ipsum"])
         {:tag :text
          :attributes {}
          :content ["Lorem Ipsum"]})))

(deftest test-tag-content-text-multiple
  (is (= (html-data [:div "foo" "bar"])
         {:tag :div
          :attributes {}
          :content ["foobar"]})))

(deftest test-tag-content-text-list
  (is (= (html-data [:div (list "foo" "bar")])
         {:tag :div
          :attributes {}
          :content ["foobar"]})))

(deftest test-tag-content-node
  (is (= (html-data [:div [:p]])
         {:tag :div
          :attributes {}
          :content [{:tag :p :attributes {} :content []}]})))

(deftest test-tag-content-nodes
  (is (= (html-data [:div [:p] [:br]])
         {:tag :div
          :attributes {}
          :content
          [{:tag :p :attributes {} :content []}
           {:tag :br :attributes {} :content []}]})))

(deftest test-tag-content-nodes-nested
  (is (= (html-data [:p [:span [:a "foo"]]])
         {:tag :p
          :attributes {}
          :content
          [{:tag :span
            :attributes {}
            :content [{:tag :a :attributes {} :content ["foo"]}]}]})))

(deftest test-tag-attributes
  (testing "tag with blank attribute map"
    (is (= (html-data [:div {}])
           {:tag :div
            :attributes {}
            :content []})))

  (testing "tag with populated attribute map"
    (is (= (html-data [:div {:min "1" :max "2"}])
           {:tag :div
            :attributes {:min "1" :max "2"}
            :content []}))
    (is (= (html-data [:img {"id" "foo"}])
           {:tag :img
            :attributes {:id "foo"}
            :content []}))
    (is (= (html-data [:img {:id "foo"}])
           {:tag :img
            :attributes {:id "foo"}
            :content []}))
    (let [id :id]
      (is (= (html-data [:img {id "foo"}])
             {:tag :img
              :attributes {:id "foo"}
              :content []}))))

  (testing "attribute values are escaped"
    (is (= (html-data [:div {:id "\""}])
           {:tag :div
            :attributes {:id "\""}
            :content []})))

  (testing "nil attributes"
    (is (= (html-data [:span {:class nil} "foo"])
           {:tag :span
            :attributes {}
            :content ["foo"]})))

  (testing "interpreted attributes"
    (let [attr-fn (constantly {:id "a" :class "b" :http-equiv "refresh"})]
      (is (= (html-data [:span (attr-fn) "foo"])
             {:tag :span
              :attributes {:id "a" :http-equiv "refresh" :class "b"}
              :content ["foo"]}))))

  (testing "tag with aria attributes"
    (is (= (html-data [:div {:aria-disabled true}])
           {:tag :div
            :attributes {:aria-disabled "true"}
            :content []})))

  (testing "tag with data attributes"
    (is (= (html-data [:div {:data-toggle "modal" :data-target "#modal"}])
           {:tag :div
            :attributes {:data-toggle "modal" :data-target "#modal"}
            :content []}))))

(deftest test-tag-attributes-boolean
  (is (= (html-data [:div {:aria-hidden true}])
         {:tag :div
          :attributes {:aria-hidden "true"}
          :content []}))
  (is (= (html-data [:div {:aria-hidden false}])
         {:tag :div
          :attributes {:aria-hidden "false"}
          :content []})))

(deftest test-compiled-tags
  (testing "tag content can be vars"
    (let [x "foo"]
      (is (= (html-data [:span x])
             {:tag :span
              :attributes {}
              :content ["foo"]}))))

  (testing "tag content can be forms"
    (is (= (html-data [:span (str (+ 1 1))])
           {:tag :span
            :attributes {}
            :content ["2"]}))
    (is (= (html-data [:span ({:foo "bar"} :foo)])
           {:tag :span
            :attributes {}
            :content ["bar"]})))

  (testing "attributes can contain vars"
    (let [id "id"]
      (is (= (html-data [:div {:id id}])
             {:tag :div
              :attributes {:id "id"}
              :content []}))
      (is (= (html-data [:div {id "id"}])
             {:tag :div
              :attributes {:id "id"}
              :content []}))
      (is (= (html-data [:div {:id id} "bar"])
             {:tag :div
              :attributes {:id "id"}
              :content ["bar"]}))))

  (testing "attributes are evaluated"
    (is (= (html-data [:img {:src (str "/foo" "/bar")}])
           {:tag :img
            :attributes {:src "/foo/bar"}
            :content []}))
    (is (= (html-data [:div {:id (str "a" "b")} (str "foo")])
           {:tag :div
            :attributes {:id "ab"}
            :content ["foo"]})))

  (testing "optimized forms"
    (is (= (html-data [:ul (for [n (range 3)] [:li {:key n} n])])
           {:tag :ul
            :attributes {}
            :content
            [{:tag :li :attributes {} :content ["0"]}
             {:tag :li :attributes {} :content ["1"]}
             {:tag :li :attributes {} :content ["2"]}]}))
    (is (= (html-data [:div (if true [:span "foo"] [:span "bar"])])
           {:tag :div
            :attributes {}
            :content [{:tag :span :attributes {} :content ["foo"]}]})))

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
  (is (= (html-data [:input])
         {:tag :input
          :attributes {}
          :content []})))

(deftest test-input-with-extra-attrs
  (is (= (html-data [:input {:class "classy"}])
         {:tag :input
          :attributes {:class "classy"}
          :content []})))

(deftest test-hidden-field
  (is (= (html-data (html/hidden-field :foo "bar"))
         {:tag :input
          :attributes
          {:type "hidden"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-hidden-field-with-extra-attrs
  (is (= (html-data (html/hidden-field {:class "classy"} :foo "bar"))
         {:tag :input
          :attributes
          {:type "hidden"
           :name "foo"
           :id "foo"
           :value "bar"
           :class "classy"}
          :content []})))

(deftest test-text-field-uncontrolled
  (is (= (html-data (html/text-field :foo))
         {:tag :input
          :attributes
          {:type "text"
           :name "foo"
           :id "foo"}
          :content []})))

(deftest test-text-field-controlled
  (is (= (html-data (html/text-field {:on-change identity} :foo ""))
         {:tag :input
          :attributes
          {:type "text"
           :name "foo"
           :id "foo"
           :value ""}
          :content []}))
  (is (= (html-data (html/text-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "text"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-text-field-with-extra-attrs
  (is (= (html-data (html/text-field
                     {:class "classy"
                      :on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "text"
           :name "foo"
           :id "foo"
           :value "bar"
           :class "classy"}
          :content []})))

(deftest test-check-box-uncontrolled
  (is (= (html-data (html/check-box :foo))
         {:tag :input
          :attributes
          {:type "checkbox"
           :name "foo"
           :id "foo"}
          :content []})))

(deftest test-check-box-controlled-checked
  (is (= (html-data (html/check-box {:on-change identity} :foo true))
         {:tag :input
          :attributes
          {:type "checkbox"
           :name "foo"
           :id "foo"
           :checked ""}
          :content []})))

(deftest test-check-box-controlled-unchecked
  (is (= (html-data (html/check-box {:on-change identity} :foo false))
         {:tag :input
          :attributes
          {:type "checkbox"
           :name "foo"
           :id "foo"}
          :content []})))

(deftest test-check-box-controlled-value
  (is (= (html-data (html/check-box {:on-change identity} :foo true "x"))
         {:tag :input
          :attributes
          {:type "checkbox"
           :name "foo"
           :id "foo"
           :checked ""
           :value "x"}
          :content []})))

(deftest test-check-box-with-extra-attrs
  (is (= (html-data (html/check-box {:class "classy"} :foo))
         {:tag :input
          :attributes
          {:type "checkbox"
           :name "foo"
           :id "foo"
           :class "classy"}
          :content []})))

(deftest test-password-field-uncontrolled
  (is (= (html-data (html/password-field :foo))
         {:tag :input
          :attributes
          {:type "password"
           :name "foo"
           :id "foo"}
          :content []})))

(deftest test-password-field-controlled
  (is (= (html-data (html/password-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "password"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-password-field-with-extra-attrs
  (is (= (html-data (html/password-field
                     {:class "classy"
                      :on-change identity}
                     :foo "bar"))
         {:tag :input
          :attributes
          {:type "password"
           :name "foo"
           :id "foo"
           :value "bar"
           :class "classy"}
          :content []})))

(deftest test-email-field-uncontrolled
  (is (= (html-data (html/email-field :foo))
         {:tag :input
          :attributes
          {:type "email"
           :name "foo"
           :id "foo"}
          :content []})))

(deftest test-email-field-controlled
  (is (= (html-data (html/email-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "email"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-search-field-uncontrolled
  (is (= (html-data (html/search-field :foo))
         {:tag :input
          :attributes
          {:type "search"
           :name "foo"
           :id "foo"}
          :content []})))

(deftest test-search-field-controlled
  (is (= (html-data (html/search-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "search"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-url-field-uncontrolled
  (is (= (html-data (html/url-field :foo))
         {:tag :input
          :attributes
          {:type "url"
           :name "foo"
           :id "foo"}
          :content []})))

(deftest test-url-field-controlled
  (is (= (html-data (html/url-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "url"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-tel-field
  (is (= (html-data (html/tel-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "tel"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-number-field
  (is (= (html-data (html/number-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "number"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-range-field
  (is (= (html-data (html/range-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "range"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-date-field
  (is (= (html-data (html/date-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "date"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-month-field
  (is (= (html-data (html/month-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "month"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-week-field
  (is (= (html-data (html/week-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "week"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-time-field
  (is (= (html-data (html/time-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "time"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-datetime-field
  (is (= (html-data (html/datetime-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "datetime"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-datetime-local-field
  (is (= (html-data (html/datetime-local-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "datetime-local"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-color-field
  (is (= (html-data (html/color-field {:on-change identity} :foo "bar"))
         {:tag :input
          :attributes
          {:type "color"
           :name "foo"
           :id "foo"
           :value "bar"}
          :content []})))

(deftest test-email-field-with-extra-attrs
  (is (= (html-data (html/email-field
                     {:class "classy"
                      :on-change identity}
                     :foo "bar"))
         {:tag :input
          :attributes
          {:type "email"
           :name "foo"
           :id "foo"
           :value "bar"
           :class "classy"}
          :content []})))

(deftest test-radio-button
  (is (= (html-data (html/radio-button {:on-change identity} :foo true 1))
         {:tag :input
          :attributes
          {:type "radio"
           :name "foo"
           :id "foo-1"
           :value "1"
           :checked ""}
          :content []})))

(deftest test-radio-button-with-extra-attrs
  (is (= (html-data (html/radio-button
                     {:class "classy"
                      :on-change identity}
                     :foo true 1))
         {:tag :input
          :attributes
          {:type "radio"
           :name "foo"
           :id "foo-1"
           :value "1"
           :checked ""
           :class "classy"}
          :content []})))

(deftest test-select-options
  (are [x y] (= x y)
    (html-data [:select (html/select-options ["foo" "bar" "baz"])])
    {:tag :select
     :attributes {}
     :content
     [{:tag :option
       :attributes {:value "foo"}
       :content ["foo"]}
      {:tag :option
       :attributes {:value "bar"}
       :content ["bar"]}
      {:tag :option
       :attributes {:value "baz"}
       :content ["baz"]}]}

    (html-data [:select (html/select-options [["Foo" 1] ["Bar" 2]])])
    {:tag :select
     :attributes {}
     :content
     [{:tag :option
       :attributes {:value "1"}
       :content ["Foo"]}
      {:tag :option
       :attributes {:value "2"}
       :content ["Bar"]}]}

    (html-data [:select (html/select-options [["Foo" 1 true] ["Bar" 2]])])
    {:tag :select
     :attributes {}
     :content
     [{:tag :option
       :attributes {:disabled "" :value "1"}
       :content ["Foo"]}
      {:tag :option
       :attributes {:value "2"}
       :content ["Bar"]}]}

    (html-data [:select (html/select-options [["Foo" [1 2]] ["Bar" [3 4]]])])
    {:tag :select
     :attributes {}
     :content
     [{:tag :optgroup
       :attributes {:label "Foo"}
       :content
       [{:tag :option
         :attributes {:value "1"}
         :content ["1"]}
        {:tag :option
         :attributes {:value "2"}
         :content ["2"]}]}
      {:tag :optgroup
       :attributes {:label "Bar"}
       :content
       [{:tag :option
         :attributes {:value "3"}
         :content ["3"]}
        {:tag :option
         :attributes {:value "4"}
         :content ["4"]}]}]}

    (html-data [:select (html/select-options [["Foo" [["bar" 1] ["baz" 2]]]])])
    {:tag :select
     :attributes {}
     :content
     [{:tag :optgroup
       :attributes {:label "Foo"}
       :content
       [{:tag :option
         :attributes {:value "1"}
         :content ["bar"]}
        {:tag :option
         :attributes {:value "2"}
         :content ["baz"]}]}]}))

(deftest test-drop-down
  (let [options ["op1" "op2"], selected "op1"
        select-options (html/select-options options)]
    (is (= (html-data (html/drop-down :foo options selected))
           {:tag :select
            :attributes {:name "foo" :id "foo"}
            :content
            [{:tag :option
              :attributes {:value "op1"}
              :content ["op1"]}
             {:tag :option
              :attributes {:value "op2"}
              :content ["op2"]}]}))))

(deftest test-drop-down-with-extra-attrs
  (let [options ["op1" "op2"], selected "op1"
        select-options (html/select-options options)]
    (is (= (html-data (html/drop-down {:class "classy"} :foo options selected))
           {:tag :select
            :attributes
            {:name "foo"
             :id "foo"
             :class "classy"}
            :content
            [{:tag :option
              :attributes {:value "op1"}
              :content ["op1"]}
             {:tag :option
              :attributes {:value "op2"}
              :content ["op2"]}]}))))

(deftest test-text-area
  (is (= (html-data (html [:textarea]))
         {:tag :textarea
          :attributes {}
          :content []}))
  (is (= (html-data (html/text-area :foo))
         {:tag :textarea
          :attributes
          {:name "foo"
           :id "foo"}
          :content []}))
  (is (= (html-data (html/text-area {:on-change identity} :foo ""))
         {:tag :textarea
          :attributes
          {:name "foo"
           :id "foo"}
          :content []}))
  (is (= (html-data (html/text-area {:on-change identity} :foo "bar"))
         {:tag :textarea
          :attributes
          {:name "foo"
           :id "foo"}
          :content ["bar"]})))

(deftest test-text-area-field-with-extra-attrs
  (is (= (html-data (html/text-area
                     {:class "classy"
                      :on-change identity} :foo "bar"))
         {:tag :textarea
          :attributes
          {:name "foo"
           :id "foo"
           :class "classy"}
          :content ["bar"]})))

(deftest test-text-area-escapes
  (is (= (html-data (html/text-area {:on-change identity} :foo "bar</textarea>"))
         {:tag :textarea
          :attributes
          {:name "foo"
           :id "foo"}
          :content ["bar</textarea>"]})))

(deftest test-file-field
  (is (= (html-data (html/file-upload :foo))
         {:tag :input
          :attributes
          {:type "file"
           :name "foo"
           :id "foo"}
          :content []})))

(deftest test-file-field-with-extra-attrs
  (is (= (html-data (html/file-upload {:class "classy"} :foo))
         {:tag :input
          :attributes
          {:type "file"
           :name "foo"
           :id "foo"
           :class "classy"}
          :content []})))

(deftest test-label
  (is (= (html-data (html/label :foo "bar"))
         {:tag :label
          :attributes {:for "foo"}
          :content ["bar"]})))

(deftest test-label-with-extra-attrs
  (is (= (html-data (html/label {:class "classy"} :foo "bar"))
         {:tag :label
          :attributes
          {:for "foo"
           :class "classy"}
          :content ["bar"]})))

(deftest test-submit
  (is (= (html-data (html/submit-button "bar"))
         {:tag :input
          :attributes
          {:type "submit"
           :value "bar"}
          :content []})))

(deftest test-submit-button-with-extra-attrs
  (is (= (html-data (html/submit-button {:class "classy"} "bar"))
         {:tag :input
          :attributes
          {:type "submit"
           :value "bar"
           :class "classy"}
          :content []})))

(deftest test-reset-button
  (is (= (html-data (html/reset-button "bar"))
         {:tag :input
          :attributes
          {:type "reset"
           :value "bar"}
          :content []})))

(deftest test-reset-button-with-extra-attrs
  (is (= (html-data (html/reset-button {:class "classy"} "bar"))
         {:tag :input
          :attributes
          {:type "reset"
           :value "bar"
           :class "classy"}
          :content []})))

(deftest test-form-to
  (is (= (html-data (html/form-to [:post "/path"] "foo" "bar"))
         {:tag :form
          :attributes
          {:method "POST"
           :action "/path"}
          :content ["foobar"]})))

(deftest test-form-to-with-hidden-method
  (is (= (html-data (html/form-to [:put "/path"] "foo" "bar"))
         {:tag :form
          :attributes
          {:method "POST"
           :action "/path"}
          :content
          [{:tag :input
            :attributes
            {:type "hidden"
             :name "_method"
             :id "_method"
             :value "PUT"}
            :content []} "foobar"]})))

(deftest test-form-to-with-extr-atts
  (is (= (html-data (html/form-to {:class "classy"} [:post "/path"] "foo" "bar"))
         {:tag :form
          :attributes
          {:method "POST"
           :action "/path"
           :class "classy"}
          :content ["foobar"]})))

(deftest test-with-group
  (testing "hidden-field"
    (is (= (html-data [:form (with-group :foo (html/hidden-field {:key 0} :bar "val"))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :input
              :attributes
              {:type "hidden"
               :name "foo[bar]"
               :id "foo-bar"
               :value "val"}
              :content []}]})))

  (testing "text-field"
    (is (= (html-data [:form (with-group :foo (html/text-field {:key 0} :bar))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :input
              :attributes
              {:type "text"
               :name "foo[bar]"
               :id "foo-bar"}
              :content []}]})))

  (testing "checkbox"
    (is (= (html-data [:form (with-group :foo (html/check-box {:key 0} :bar))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :input
              :attributes
              {:type "checkbox"
               :name "foo[bar]"
               :id "foo-bar"}
              :content []}]})))

  (testing "password-field"
    (is (= (html-data [:form (with-group :foo (html/password-field {:key 0} :bar))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :input
              :attributes
              {:type "password"
               :name "foo[bar]"
               :id "foo-bar"}
              :content []}]})))

  (testing "radio-button"
    (is (= (html-data [:form (with-group :foo (html/radio-button {:key 0} :bar false "val"))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :input
              :attributes
              {:type "radio"
               :name "foo[bar]"
               :id "foo-bar-val"
               :value "val"}
              :content []}]})))

  (testing "drop-down"
    (is (= (html-data [:form (with-group :foo (html/drop-down {:key 0} :bar []))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :select
              :attributes
              {:name "foo[bar]"
               :id "foo-bar"}
              :content []}]})))

  (testing "text-area"
    (is (= (html-data [:form (with-group :foo (html/text-area {:key 0 :on-change identity} :bar "baz"))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :textarea
              :attributes
              {:name "foo[bar]"
               :id "foo-bar"}
              :content ["baz"]}]})))

  (testing "file-upload"
    (is (= (html-data [:form (with-group :foo (html/file-upload {:key 0} :bar))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :input
              :attributes
              {:type "file"
               :name "foo[bar]"
               :id "foo-bar"}
              :content []}]})))

  (testing "label"
    (is (= (html-data [:form (with-group :foo (html/label {:key 0} :bar "Bar"))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :label
              :attributes {:for "foo-bar"}
              :content ["Bar"]}]})))

  (testing "multiple with-groups"
    (is (= (html-data [:form (with-group :foo (with-group :bar (html/text-field {:key 0} :baz)))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :input
              :attributes
              {:type "text"
               :name "foo[bar][baz]"
               :id "foo-bar-baz"}
              :content []}]})))

  (testing "multiple elements"
    (is (= (html-data [:form (with-group :foo
                               (html/label {:key 0} :bar "Bar")
                               (html/text-field {:key 1} :var))])
           {:tag :form
            :attributes {}
            :content
            [{:tag :label
              :attributes {:for "foo-bar"}
              :content ["Bar"]}
             {:tag :input
              :attributes
              {:type "text"
               :name "foo[var]"
               :id "foo-var"}
              :content []}]}))))

(deftest test-merge-attributes-let
  (let [classes (merge {:id "a"} {:class "b"})]
    (is (= (html-data [:div classes "content"])
           {:tag :div
            :attributes {:id "a" :class "b"}
            :content ["content"]}))))

(deftest test-issue-2-merge-class
  (is (= (html-data [:div.a {:class (if (true? true) "true" "false")}])
         {:tag :div
          :attributes {:class "a true"}
          :content []}))
  (is (= (html-data [:div.a.b {:class (if (true? true) ["true"] "false")}])
         {:tag :div
          :attributes {:class "a b true"}
          :content []})))

(deftest test-issue-3-recursive-js-value
  (is (= (html-data [:div.interaction-row {:style {:position "relative"}}])
         {:tag :div
          :attributes {:style "position:relative;" :class "interaction-row"}
          :content []}))
  (let [username "foo", hidden #(if %1 {:display "none"} {:display "block"})]
    (is (= (html-data [:ul.nav.navbar-nav.navbar-right.pull-right
                       [:li.dropdown {:style (hidden (nil? username))}
                        [:a.dropdown-toggle {:role "button" :href "#"} (str "Welcome, " username)
                         [:span.caret]]
                        [:ul.dropdown-menu {:role "menu" :style {:left 0}}]]])
           {:tag :ul
            :attributes {:class "nav navbar-nav navbar-right pull-right"}
            :content
            [{:tag :li
              :attributes {:style "display:block;" :class "dropdown"}
              :content
              [{:tag :a
                :attributes {:role "button" :href "#" :class "dropdown-toggle"}
                :content
                ["Welcome, foo"
                 {:tag :span :attributes {:class "caret"} :content []}]}
               {:tag :ul
                :attributes {:role "menu" :style "left:0;" :class "dropdown-menu"}
                :content []}]}]}))))

(deftest test-issue-22-id-after-class
  (is (= (html-data [:div.well#setup])
         {:tag :div
          :attributes {:id "setup" :class "well"}
          :content []})))

(deftest test-issue-23-conditionals
  (are [form expected] (= form expected)
    (html-data (let [x true] (when x [:div])))
    {:tag :div
     :attributes {}
     :content []}

    (html-data (let [x false] (when x [:div])))
    nil

    (html-data (let [x false] (when-not x [:div])))
    {:tag :div
     :attributes {}
     :content []}

    (html-data (let [x true] (when-not x [:div (str x)])))
    nil

    (html-data (let [x true] (if-not x [:div])))
    nil

    (html-data (let [x false] (if-not x [:div])))
    {:tag :div
     :attributes {}
     :content []}

    (let [x true] (html-data (if-not x [:div])))
    nil

    (let [x false] (html-data (if-not x [:div])))
    {:tag :div
     :attributes {}
     :content []}

    (html-data [:div (if true {:class "test"})])
    {:tag :div
     :attributes {:class "test"}
     :content []}

    (html-data [:div (when true {:class "test"})])
    {:tag :div
     :attributes {:class "test"}
     :content []}

    (html-data [:div (if-not false {:class "test"})])
    {:tag :div
     :attributes {:class "test"}
     :content []}

    (html-data [:div (when-not false {:class "test"})])
    {:tag :div
     :attributes {:class "test"}
     :content []}

    (let [x 1] (html-data (when x [:div x])))
    {:tag :div
     :attributes {}
     :content ["1"]}

    (let [x 1] (html-data (when-not x [:div x])))
    nil))

(deftest test-issue-24-attr-and-keyword-classes
  (let [style-it (fn [p] {:placeholder (str p) :type "text"})]
    (is (= (html-data [:input.helloworld (style-it "dinosaurs")])
           {:tag :input
            :attributes
            {:type "text"
             :placeholder "dinosaurs"
             :class "helloworld"}
            :content []}))))

(deftest test-issue-25-comma-separated-class
  (is (= (html-data [:div.c1.c2 "text"])
         {:tag :div
          :attributes {:class "c1 c2"}
          :content ["text"]}))
  (is (= (html-data [:div.aa (merge {:class "bb"})])
         {:tag :div
          :attributes {:class "aa bb"}
          :content []}))
  (is (= (let [input-classes ["large" "big"]]
           (html-data [:input.form-control
                       (merge {:class input-classes})]))
         {:tag :input
          :attributes {:class "form-control large big"}
          :content []})))

(deftest test-issue-33-number-warning
  (is (= (html-data [:div (count [1 2 3])])
         {:tag :div :attributes {} :content ["3"]})))

(deftest test-issue-37-camel-case-style-attrs
  (is (= (html-data [:div {:style {:z-index 1000}}])
         {:tag :div
          :attributes {:style "z-index:1000;"}
          :content []}))
  (is (= (html-data [:div (merge {:style {:z-index 1000}})])
         {:tag :div
          :attributes {:style "z-index:1000;"}
          :content []})))

(deftest test-div-with-nested-lazy-seq
  (is (= (html-data [:div (map identity ["A" "B"])])
         {:tag :div :attributes {} :content ["AB"]})))

(deftest test-div-with-nested-list
  (is (= (html-data [:div (list "A" "B")])
         {:tag :div :attributes {} :content ["AB"]})))

(deftest test-div-with-nested-vector
  (is (= (html-data [:div ["A" "B"]])
         {:tag :div :attributes {} :content ["AB"]}))
  (is (= (html-data [:div (vector"A" "B")])
         {:tag :div :attributes {} :content ["AB"]})))

(deftest test-class-duplication
  (is (= (html-data [:div.a.a.b.b.c {:class "c"}])
         {:tag :div
          :attributes {:class "a a b b c c"}
          :content []})))

(deftest test-class-order
  (is (= (html-data [:div.a.b.c {:class "d"}])
         {:tag :div
          :attributes {:class "a b c d"}
          :content []}))
  (is (= (html-data [:div.a.b.c {:class ["foo" "bar"]}])
         {:tag :div
          :attributes {:class "a b c foo bar"}
          :content []})))

(deftest test-class-as-set
  (is (= (html-data [:div {:class #{"a" "b" "c"}}])
         {:tag :div
          :attributes {:class "a b c"}
          :content []}))
  (is (= (html-data [:div {:class (set ["a" "b" "c"])}])
         {:tag :div
          :attributes {:class "a b c"}
          :content []})))

(deftest test-class-as-list
  (is (= (html-data [:div {:class '("a" "b" "c")}])
         {:tag :div
          :attributes {:class "a b c"}
          :content []}))
  (is (= (html-data [:div {:class (list "a" "b" "c")}])
         {:tag :div
          :attributes {:class "a b c"}
          :content []})))

(deftest test-class-as-vector
  (is (= (html-data [:div {:class ["a" "b" "c"]}])
         {:tag :div
          :attributes {:class "a b c"}
          :content []}))
  (is (= (html-data [:div {:class (vector "a" "b" "c")}])
         {:tag :div
          :attributes {:class "a b c"}
          :content []})))

(deftest test-issue-80
  (is (= (html-data
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
         {:tag :div
          :attributes {}
          :content
          [{:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}
           {:tag :div :attributes {:class "foo bar"} :content []}]})))

(deftest test-issue-90
  (is (= (html-data [:div nil (case :a :a "a")])
         {:tag :div
          :attributes {}
          :content ["a"]})))

(deftest test-complex-scenario
  (is (= (html-data [:div.a {:class (list "b")} (case :a :a "a")])
         {:tag :div
          :attributes {:class "a b"}
          :content ["a"]})))

(deftest test-issue-57
  (let [payload {:username "john" :likes 2}]
    (is (= (html-data
            (let [{:keys [username likes]} payload]
              [:div
               [:div (str username " (" likes ")")]
               [:div "!Pixel Scout"]]))
           {:tag :div
            :attributes {}
            :content
            [{:tag :div :attributes {} :content ["john (2)"]}
             {:tag :div :attributes {} :content ["!Pixel Scout"]}]}))))

(rum/defc issue-57-rum [text]
  (html
   (let [text-add (str text " warning")]
     [:div
      [:h1 text]
      [:h1 text-add]])))

(deftest test-issue-57-rum
  (is (= (html-data (issue-57-rum "This gives"))
         {:tag :div
          :attributes {}
          :content
          [{:tag :h1 :attributes {} :content ["This gives"]}
           {:tag :h1 :attributes {} :content ["This gives warning"]}]})))

(deftest test-issue-115
  (is (= (html-data [:a {:id :XY}])
         {:tag :a
          :attributes {:id "XY"}
          :content []}))
  (is (= (html-data [:a (identity {:id :XY})])
         {:tag :a
          :attributes {:id "XY"}
          :content []})))

(deftest test-issue-130
  (let [css {:table-cell "bg-blue"}]
    (is (= (html-data [:div {:class (:table-cell css)} [:span "abc"]])
           {:tag :div
            :attributes {:class "bg-blue"}
            :content
            [{:tag :span
              :attributes {}
              :content ["abc"]}]}))))

(deftest test-issue-158
  (let [focused? true]
    (is (= (html-data [:div {:style (merge {:margin-left "2rem"}
                                           (when focused? {:color "red"}))}])
           {:tag :div
            :attributes
            {:style "margin-left:2rem;color:red;"}
            :content []})))
  (let [focused? false]
    (is (= (html-data [:div {:style (merge {:margin-left "2rem"}
                                           (when focused? {:color "red"}))}])
           {:tag :div
            :attributes
            {:style "margin-left:2rem;"}
            :content []}))))
