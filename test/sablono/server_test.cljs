(ns sablono.server-test
  (:require [cljs.test :as t :refer-macros [are is deftest]]
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

(deftest test-literal-tag-child
  (let [x "a"]
    (is (= (server/render-static (html [:div x "b"]))
           "<div>ab</div>"))))

(deftest test-literal-tag-map
  (let [x {:class "a"}]
    (is (= (server/render-static (html [:div x "b"]))
           "<div class=\"a\">b</div>"))))
