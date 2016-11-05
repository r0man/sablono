(ns sablono.render-test
  (:require [clojure.test :refer [deftest is] ]
            [sablono.interpreter :refer [interpret]]
            [sablono.render :refer [render-html]]
            [sablono.server :as server]))

(deftest test-render
  (is (= (render-html [:div])
         (server/render (interpret [:div])))))

