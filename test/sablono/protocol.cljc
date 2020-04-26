(ns sablono.protocol)

(defprotocol IReactDOMElement
  (^String -render-to-string [this react-id ^StringBuilder sb]
   "Render a DOM node to string."))

(defprotocol IReactComponent
  (-render [this] "Return a valid ReactDOMElement."))
