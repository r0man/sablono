(ns sablono.server
  (:require [react :refer [createElement]]
            ["react-dom/server" :refer [renderToString renderToStaticMarkup]]))

(defn render
  "Render `element` as HTML string."
  [element]
  (when element (renderToString element)))

(defn render-static
  "Render `element` as HTML string, without React internal attributes."
  [element]
  (when element (renderToStaticMarkup element)))
