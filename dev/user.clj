(ns user
  (:require [figwheel-sidecar.repl-api :as figwheel]))

(defn go []
  (figwheel/start-figwheel!)
  (figwheel/cljs-repl))

(defn stop []
  (figwheel/stop-figwheel!))
