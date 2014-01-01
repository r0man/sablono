(ns sablono.render-test
  #+clj (:import cljs.tagged_literals.JSValue)
  #+cljs (:require-macros [cemerick.cljs.test :refer [are is deftest testing]])
  (:require [sablono.render :as r]
            #+clj [clojure.test :refer :all]
            #+cljs [cemerick.cljs.test :as t]))
