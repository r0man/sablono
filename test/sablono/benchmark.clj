(ns sablono.benchmark
  (:require [clojure.test :refer [deftest]]
            [criterium.core :refer :all]
            [sablono.normalize :as normalize]))

(deftest ^:benchmark normalize-element
  (println "Benchmark normalize-element ...")
  (with-progress-reporting
    (quick-bench
     (normalize/element
      [:div.a
       #{:class "b"}
       [:div.c "c"]
       [:div.d "d"]])
     :verbose)))
