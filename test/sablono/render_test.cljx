(ns sablono.render-test
  #+clj (:import cljs.tagged_literals.JSValue)
  #+cljs (:require-macros [cemerick.cljs.test :refer [are is deftest testing]])
  (:require [sablono.render :as r]
            #+clj [clojure.test :refer :all]
            #+cljs [cemerick.cljs.test :as t]))

#+clj
(deftest test-to-js
  (let [v (r/to-js [])]
    (is (instance? JSValue v))
    (is (= [] (.val v))))
  (let [v (r/to-js {})]
    (is (instance? JSValue v))
    (is (= {} (.val v))))
  (let [v (r/to-js [1 [2] {:a 1 :b {:c [2 [3]]}}])]
    (is (instance? JSValue v))
    (is (= 1 (first (.val v))))
    (is (= [2] (.val (second (.val v)))))
    (let [v (nth (.val v) 2)]
      (is (instance? JSValue v))
      (is (= 1 (:a (.val v))))
      (let [v (:b (.val v))]
        (is (instance? JSValue v))
        (let [v (:c (.val v))]
          (is (instance? JSValue v))
          (is (= 2 (first (.val v))))
          (is (= [3] (.val (second (.val v))))))))))

(deftest test-merge-with-class
  (is (= {:a 1 :b 2} (r/merge-with-class {:a 1} {:b 2})))
  (is (= {:a 1 :b 2 :c 3 :className [:a "b" "c"]}
         (r/merge-with-class
          {:a 1 :className :a}
          {:b 2 :className "b"}
          {:c 3 :className ["c"]}))))
