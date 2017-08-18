(ns sablono.server
  (:require ["react-dom/server" :as react-dom-server]))

(defn render
  "Render `element` as HTML string."
  [element]
  (if element
    (react-dom-server/renderToString element)))

(defn render-static
  "Render `element` as HTML string, without React internal attributes."
  [element]
  (if element
    (react-dom-server/renderToStaticMarkup element)))
