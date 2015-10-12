(ns sablono.test
  (:require [cljs.test :as test :refer-macros [run-tests] :refer [report]]
            [sablono.benchmark]
            [sablono.core-test]
            [sablono.interpreter-test]
            [sablono.interpreter2-test]
            [sablono.parser-test]
            [sablono.util-test]))

(defmethod report [::test/default :summary] [m]
  (println "\nRan" (:test m) "tests containing"
           (+ (:pass m) (:fail m) (:error m)) "assertions.")
  (println (:fail m) "failures," (:error m) "errors.")
  (aset js/window "test-failures" (+ (:fail m) (:error m))))

(defn ^:export main
  "Run all tests."
  []
  (enable-console-print!)
  (test/run-tests
   'sablono.core-test
   'sablono.interpreter-test
   'sablono.interpreter2-test
   'sablono.parser-test
   'sablono.util-test
   ;; 'sablono.benchmark
   ))
