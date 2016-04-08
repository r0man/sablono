(ns sablono.server-test
  (:require [cljs.test :as t :refer-macros [are deftest]]
            [sablono.core :refer-macros [html]]
            [sablono.server :as server]))

(deftest test-render
  (are [markup match]
      (re-matches (re-pattern match) (server/render markup))
    (html [:div#a.b "c"])
    "<div id=\"a\" class=\"b\" data-reactroot=\"\" data-reactid=\".*\" data-react-checksum=\".*\">c</div>"
    (html [:div (when true [:p "data"]) (if true [:p "data"] nil)])
    "<div data-reactroot=\"\" data-reactid=\".*\" data-react-checksum=\".*\"><p data-reactid=\".*\">data</p><p data-reactid=\".*\">data</p></div>"))

(deftest test-render-static
  (are [markup expected]
      (= expected (server/render-static markup))
    (html [:div#a.b "c"])
    "<div id=\"a\" class=\"b\">c</div>"
    (html [:div (when true [:p "data"]) (if true [:p "data"] nil)])
    "<div><p>data</p><p>data</p></div>"))
