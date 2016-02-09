(ns sablono.server
  (:require [cljsjs.react.dom.server]))

(defn render
  "Render `element` as HTML string."
  [element]
  (if element
    (js/ReactDOMServer.renderToString element)))

(defn render-static
  "Render `element` as HTML string, without React internal attributes."
  [element]
  (if element
    (js/ReactDOMServer.renderToStaticMarkup element)))
