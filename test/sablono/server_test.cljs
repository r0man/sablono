(ns sablono.server-test
  (:require [cljs.test :as t :refer-macros [are deftest]]
            [sablono.core :refer-macros [html]]
            [sablono.server :as server]))

(deftest test-render
  (are [markup expected]
      (= (server/render markup) expected)
    (html [:div#a.b "c"])
    "<div id=\"a\" class=\"b\" data-reactroot=\"\">c</div>"
    (html [:div (when true [:p "data"]) (if true [:p "data"] nil)])
    "<div data-reactroot=\"\"><p>data</p><p>data</p></div>"))

(deftest test-render-static
  (are [markup expected]
      (= expected (server/render-static markup))
    (html [:div#a.b "c"])
    "<div id=\"a\" class=\"b\">c</div>"
    (html [:div (when true [:p "data"]) (if true [:p "data"] nil)])
    "<div><p>data</p><p>data</p></div>"))
