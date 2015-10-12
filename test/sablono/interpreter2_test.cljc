(ns sablono.interpreter2-test
  (:require [sablono.interpreter2 :refer [html]]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [are deftest is]]))
  #?(:clj (:import cljs.tagged_literals.JSValue)))

(defn unwrap-jsvalue [x]
  #?(:clj
     (if (instance? JSValue x)
       (let [v (.-val x)]
         (cond
           (sequential? v)
           (map unwrap-jsvalue v)
           (map? v)
           (->> (for [[k v] v]
                  [k (unwrap-jsvalue v)])
                (into {}))
           :else v))
       x)
     :cljs
     (js->clj x :keywordize-keys true)))

(defn === [x y]
  (= (unwrap-jsvalue x)
     (unwrap-jsvalue y)))

(deftest test-html
  (are [element expected]
      (=== expected (html element))

    1 1
    1.0 1.0
    "x" "x"

    [:div]
    {:_isReactElement true
     :type "div"
     :props nil}

    [:#a]
    {:_isReactElement true
     :type "div"
     :props {:id "a"}}

    [ :.b]
    {:_isReactElement true
     :type "div"
     :props {:className "b"}}

    [ :div#a]
    {:_isReactElement true
     :type "div"
     :props {:id "a"}}

    [ :div#a#a]
    {:_isReactElement true
     :type "div"
     :props {:id "a"}}

    [ :div.b]
    {:_isReactElement true
     :type "div"
     :props {:className "b"}}

    [:div.b.b]
    {:_isReactElement true
     :type "div"
     :props {:className "b"}}

    [ :div#a#b#c]
    {:_isReactElement true
     :type "div"
     :props {:id "a"}}

    [ :div.a.b.c]
    {:_isReactElement true
     :type "div"
     :props {:className "a b c"}}

    [ :div#a.b]
    {:_isReactElement true
     :type "div"
     :props {:id "a" :className "b"}}

    [ :div.b#a]
    {:_isReactElement true
     :type "div"
     :props {:id "a" :className "b"}}

    [ :div#a#b#c.d.e.f]
    {:_isReactElement true
     :type "div"
     :props {:className "d e f" :id "a"}}

    [:div "a"]
    {:_isReactElement true
     :type "div"
     :props {:children ["a"]}}

    [:div "a" "b" "c" [:div "d"]]
    {:_isReactElement true
     :type "div"
     :props
     {:children
      ["a" "b" "c"
       {:_isReactElement true
        :type "div"
        :props {:children ["d"]}}]}}

    ))

(deftest test-issue-24-attr-and-keyword-classes
  (let [style-it (fn [p] {:placeholder (str p) :type "text"})]
    #?(:cljs (println (js/React.renderToStaticMarkup (html [:input.helloworld (style-it "dinosaurs")]))))
    (is (=== (html [:input.helloworld (style-it "dinosaurs")])
             {:_isReactElement true
              :type "input"
              :props
              {:placeholder "dinosaurs"
               :type "text"
               :className "helloworld"}}))))

(deftest test-duplicate-class-attribute
  (is (=== (html [:div.b.b])
           {:_isReactElement true
            :type "div"
            :props {:className "b"}})))

(deftest test-duplicate-id-attribute
  (is (=== (html [:div#b#b])
           {:_isReactElement true
            :type "div"
            :props {:id "b"}})))

(deftest test-first-id-attribute-wins
  (is (=== (html [:div#a#b])
           {:_isReactElement true
            :type "div"
            :props {:id "a"}})))
