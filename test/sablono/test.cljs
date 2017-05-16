(ns sablono.test
  (:require [clojure.test.check]
            [clojure.test.check.generators]
            [cljsjs.react]
            [cljsjs.react.dom]
            [cljsjs.react.dom.server]
            [tubax.core :as tubax]
            [sablono.server :as server]))

(defn parse-xml [s]
  (tubax/xml->clj s))

(defn render-str [x]
  (server/render-static x))
