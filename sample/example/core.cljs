(ns example.core
  (:require [sablono.core :refer [html]]))

(defn ^:export main []
  (html [:div#a.b.c]))
