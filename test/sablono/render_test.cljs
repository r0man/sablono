(ns sablono.render-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop :include-macros true]
            [clojure.test.check.clojure-test :refer-macros [defspec]]
            [clojure.walk :as walk]
            [sablono.generators :as gen2]
            [sablono.normalize :as normalize]
            [sablono.interpreter :refer [interpret]]
            [sablono.render :refer [render-html]]
            [sablono.server :as server]
            [sablono.util :as util]))

(defn rename-attributes [node]
  (walk/prewalk
   (fn [node]
     (if (and (vector? node)
              (keyword? (first node))
              (map? (second node)))
       [(first node)
        (normalize/html-to-dom-attrs (second node))
        (nth node 2 nil)]
       node))
   node))

(defn render-react [node]
  (render-html (rename-attributes node)))

(defn render-sablono [node]
  (server/render (interpret node)))

(defspec test-render-element
  (prop/for-all
   [type gen2/html-types]
   (is (= (render-sablono [type])
          (render-react [type])))))

(defspec test-render-attributes
  (prop/for-all
   [type gen2/html-types
    attributes gen2/html-attributes]
   (is (= (render-sablono [type attributes])
          (render-react [type attributes])))))

#_(defspec test-render-check
    (prop/for-all
     [nodes gen2/elements]
     (is (= (render-html nodes)
            (server/render (interpret nodes))))))

;; (render-html [:div [:div [:div {:auto-play ""} ""]]])
;; (server/render (interpret [:div [:div [:div {:auto-play ""} ""]]]))

(render-react [:div {:default-checked ""}])
(render-sablono [:div {:default-checked ""}])

(server/render (js/React.createElement "div" #js {:defaultChecked true}))
(server/render (js/React.createElement "input" #js {:defaultChecked true}))
