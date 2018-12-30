(ns sablono.server
  (:require ["react-dom/server" :as ReactDOMServer]))

(defn render
  "Render `element` as HTML string."
  [element]
  (if element
    (ReactDOMServer/renderToString element)))

(defn render-static
  "Render `element` as HTML string, without React internal attributes."
  [element]
  (if element
    (ReactDOMServer/renderToStaticMarkup element)))
