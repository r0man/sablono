(ns sablono.interpreter-test
  (:require ;; [om.dom :as dom]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.string :as str]
            [clojure.test :refer [are is deftest]]
            [clojure.test.check.clojure-test #?(:clj :refer :cljs :refer-macros) [defspec]]
            [clojure.test.check.properties #?(:clj :refer :cljs :refer-macros) [for-all]]
            ;; [om.next :as om :refer [defui]]
            [sablono.core :refer [defhtml html]]
            [sablono.interpreter :as i]
            [sablono.specs :as specs]
            [sablono.test :refer [parse-xml render-str]]))

(def gen-string-child
  (gen/such-that not-empty (s/gen string?)))

(def gen-class-names
  (gen/not-empty (gen/list (s/gen ::specs/class-name))))

(defn interpret
  "Interpret `x` as a Hiccup data structure, render it as a static
  HTML string, parse it and return a Clojure data structure."
  [x]
  (some->> (i/interpret x)
           (render-str)
           (parse-xml)))

;; (deftest test-attributes
;;   (are [attrs expected]
;;       (= expected (js->clj (i/attributes attrs)))
;;     nil nil
;;     {} {}
;;     {:className ""} {}
;;     {:className "aa"}       {"className" "aa"}
;;     {:className "aa bb"}    {"className" "aa bb"}
;;     {:className ["aa bb"]}  {"className" "aa bb"}
;;     {:className '("aa bb")} {"className" "aa bb"}
;;     {:id :XY}               {"id" "XY"}))

(deftest test-short-hand-div-forms
  (is (= (interpret [:#test.klass1])
         {:tag :div
          :attributes {:id "test" :class "klass1"}
          :content []})))

(deftest test-static-children-as-arguments
  (is (= (interpret
          [:div
           [:div {:class "1" :key 1}]
           [:div {:class "2" :key 2}]])
         {:tag :div
          :attributes {}
          :content
          [{:tag :div :attributes {:class "1"} :content []}
           {:tag :div :attributes {:class "2"} :content []}]})))

(defspec test-tag-only
  (for-all [tag (s/gen ::specs/tag)]
           (= (interpret [tag])
              {:tag tag
               :attributes {}
               :content []})))

(defspec test-tag-with-id
  (for-all [tag (s/gen ::specs/tag)
            id (gen/not-empty (s/gen string?))]
           (= (interpret [(keyword (str (name tag) "#" id))])
              {:tag tag
               :attributes {:id id}
               :content []})))

(defspec test-tag-with-class
  (for-all [tag (s/gen ::specs/tag)
            class (s/gen ::specs/class-name)]
           (= (interpret [(keyword (str (name tag) "." class))])
              {:tag tag
               :attributes {:class class}
               :content []})))

(defspec test-tag-with-classes
  (for-all [tag (s/gen ::specs/tag), classes gen-class-names]
           (is (= (interpret [(keyword (str (name tag) "." (str/join "." classes)))])
                  {:tag tag
                   :attributes {:class (str/join " " classes)}
                   :content []}))))

(deftest test-class-duplication
  (is (= (interpret [:div.a.a.b.b.c {:class "c"}])
         {:tag :div
          :attributes {:class "a a b b c c"}
          :content []}))  )

(defspec test-class-as-set
  (for-all [tag (s/gen ::specs/tag), classes gen-class-names]
           (= (interpret [tag {:class (set classes)}])
              {:tag tag
               :attributes {:class (str/join " " (set classes))}
               :content []})))

(defspec test-class-as-list
  (for-all [tag (s/gen ::specs/tag), classes gen-class-names]
           (= (interpret [tag {:class (apply list classes)}])
              {:tag tag
               :attributes {:class (str/join " " classes)}
               :content []})))

(defspec test-class-as-vector
  (for-all [tag (s/gen ::specs/tag), classes gen-class-names]
           (= (interpret [tag {:class (vec classes)}])
              {:tag tag
               :attributes {:class (str/join " " classes)}
               :content []})))

(defspec test-child-as-string
  (for-all [child gen-string-child]
           (= (interpret [:div child])
              {:tag :div
               :attributes {}
               :content [child]})))

(defspec test-child-as-number
  (for-all [child (s/gen number?)]
           (= (interpret [:div child])
              {:tag :div
               :attributes {}
               :content [(str child)]})))

(defspec test-div-with-seq-child
  (for-all [children (gen/not-empty (gen/list gen-string-child))]
           (= (interpret [:div (seq children)])
              {:tag :div
               :attributes {}
               :content [(apply str children)]})))

(defspec test-div-with-list-child
  (for-all [children (gen/not-empty (gen/list gen-string-child))]
           (= (interpret [:div children])
              {:tag :div
               :attributes {}
               :content [(apply str children)]})))

(defspec test-div-with-vector-child
  (for-all [children (gen/not-empty (gen/vector gen-string-child))]
           (= (interpret [:div children])
              {:tag :div
               :attributes {}
               :content [(apply str children)]})))

(deftest test-issue-80
  (is (= (interpret
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
  (is (= (interpret [:div nil (case :a :a "a")])
         {:tag :div
          :attributes {}
          :content ["a"]})))

(deftest test-issue-57
  (let [payload {:username "john" :likes 2}]
    (is (= (interpret
            (let [{:keys [username likes]} payload]
              [:div
               [:div (str username " (" likes ")")]
               [:div "!Pixel Scout"]]))
           {:tag :div
            :attributes {}
            :content
            [{:tag :div
              :attributes {}
              :content ["john (2)"]}
             {:tag :div
              :attributes {}
              :content ["!Pixel Scout"]}]}))))

(defhtml element-a []
  [:div.a])

;; (defui ElementB
;;   Object
;;   (render [this]
;;     (html [:div.b])))

;; (def element-b (om/factory ElementB))

;; (deftest test-om-render-defhtml
;;   (is (= (interpret [:div (element-a)])
;;          {:tag :div
;;           :attributes {}
;;           :content
;;           [{:tag :div
;;             :attributes {:class "a"}
;;             :content []}]})))

;; (deftest test-om-render-defui
;;   (is (= (interpret [:div (element-b {})])
;;          {:tag :div
;;           :attributes {}
;;           :content
;;           [{:tag :div
;;             :attributes {:class "b"}
;;             :content []}]})))

;; #?(:clj (deftest test-om-render-str-defhtml
;;           (is (= (dom/render-to-str (html [:div (element-a)]))
;;                  (str "<div data-reactroot=\"\" data-reactid=\"1\" "
;;                       "data-react-checksum=\"-1277879407\">"
;;                       "<div class=\"a\" data-reactid=\"2\">"
;;                       "</div></div>")))))

;; #?(:clj (deftest test-om-render-str-defui
;;           (is (= (dom/render-to-str (html [:div (element-b {})]))
;;                  (str "<div data-reactroot=\"\" data-reactid=\"1\" "
;;                       "data-react-checksum=\"-1275782254\">"
;;                       "<div class=\"b\" data-reactid=\"2\">"
;;                       "</div></div>")))))
