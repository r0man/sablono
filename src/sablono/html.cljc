(ns sablono.html
  (:refer-clojure :exclude [map meta time])
  (:require [clojure.string :as str]
            [sablono.protocol :as p])
  #?(:cljs (:import goog.string.StringBuffer)))

(def MOD 65521)

;; Adapted from https://github.com/tonsky/rum
(defn adler32 [sb]
  (let [l (.length sb)
        m (bit-and l -4)]
    (loop [a (int 1)
           b (int 0)
           i 0
           n (min (+ i 4096) m)]
      (cond
        (< i n)
        (let [c0 (int (.charAt sb i))
              c1 (int (.charAt sb (+ i 1)))
              c2 (int (.charAt sb (+ i 2)))
              c3 (int (.charAt sb (+ i 3)))
              b  (+ b a c0
                    a c0 c1
                    a c0 c1 c2
                    a c0 c1 c2 c3)
              a  (+ a c0 c1 c2 c3)]
          (recur (rem a MOD) (rem b MOD) (+ i 4) n))

        (< i m)
        (recur a b i (min (+ i 4096) m))

        (< i l)
        (let [c0 (int (.charAt sb i))]
          (recur (+ a c0) (+ b a c0) (+ i 1) n))

        :else
        (let [a (rem a MOD)
              b (rem b MOD)]
          (bit-or (int a) (unchecked-int (bit-shift-left b 16))))))))

(defn assign-react-checksum [sb]
  (.insert sb (.indexOf sb ">") (str " data-react-checksum=\"" (adler32 sb) "\"")))

(def tags
  '[a
    abbr
    address
    area
    article
    aside
    audio
    b
    base
    bdi
    bdo
    big
    blockquote
    body
    br
    button
    canvas
    caption
    cite
    code
    col
    colgroup
    data
    datalist
    dd
    del
    details
    dfn
    dialog
    div
    dl
    dt
    em
    embed
    fieldset
    figcaption
    figure
    footer
    form
    h1
    h2
    h3
    h4
    h5
    h6
    head
    header
    hr
    html
    i
    iframe
    img
    ins
    kbd
    keygen
    label
    legend
    li
    link
    main
    map
    mark
    menu
    menuitem
    meta
    meter
    nav
    noscript
    object
    ol
    optgroup
    output
    p
    param
    picture
    pre
    progress
    q
    rp
    rt
    ruby
    s
    samp
    script
    section
    small
    source
    span
    strong
    style
    sub
    summary
    sup
    table
    tbody
    td
    tfoot
    th
    thead
    time
    title
    tr
    track
    u
    ul
    var
    video
    wbr

    ;; svg
    circle
    clipPath
    ellipse
    g
    line
    mask
    path
    pattern
    polyline
    rect
    svg
    text
    defs
    linearGradient
    polygon
    radialGradient
    stop
    tspan])

;; ===================================================================
;; Server-side rendering

;; https://github.com/facebook/react/blob/57ae3b/src/renderers/dom/shared/SVGDOMPropertyConfig.js
;; https://github.com/facebook/react/blob/57ae3b/src/renderers/dom/shared/HTMLDOMPropertyConfig.js
(def supported-attrs
  #{ ;; HTML
    "accept" "acceptCharset" "accessKey" "action" "allowFullScreen" "allowTransparency" "alt"
    "async" "autoComplete" "autoFocus" "autoPlay" "capture" "cellPadding" "cellSpacing" "challenge"
    "charSet" "checked" "cite" "classID" "className" "colSpan" "cols" "content" "contentEditable"
    "contextMenu" "controls" "coords" "crossOrigin" "data" "dateTime" "default" "defer" "dir"
    "disabled" "download" "draggable" "encType" "form" "formAction" "formEncType" "formMethod"
    "formNoValidate" "formTarget" "frameBorder" "headers" "height" "hidden" "high" "href" "hrefLang"
    "htmlFor" "httpEquiv" "icon" "id" "inputMode" "integrity" "is" "keyParams" "keyType" "kind" "label"
    "lang" "list" "loop" "low" "manifest" "marginHeight" "marginWidth" "max" "maxLength" "media"
    "mediaGroup" "method" "min" "minLength" "multiple" "muted" "name" "noValidate" "nonce" "open"
    "optimum" "pattern" "placeholder" "poster" "preload" "profile" "radioGroup" "readOnly" "referrerPolicy"
    "rel" "required" "reversed" "role" "rowSpan" "rows" "sandbox" "scope" "scoped" "scrolling" "seamless" "selected"
    "shape" "size" "sizes" "span" "spellCheck" "src" "srcDoc" "srcLang" "srcSet" "start" "step" "style" "summary"
    "tabIndex" "target" "title" "type" "useMap" "value" "width" "wmode" "wrap"
    ;; RDF
    "about" "datatype" "inlist" "prefix" "property" "resource" "typeof" "vocab"
    ;; SVG
    "accentHeight" "accumulate" "additive" "alignmentBaseline" "allowReorder" "alphabetic"
    "amplitude" "ascent" "attributeName" "attributeType" "autoReverse" "azimuth"
    "baseFrequency" "baseProfile" "bbox" "begin" "bias" "by" "calcMode" "clip"
    "clipPathUnits" "contentScriptType" "contentStyleType" "cursor" "cx" "cy" "d"
    "decelerate" "descent" "diffuseConstant" "direction" "display" "divisor" "dur"
    "dx" "dy" "edgeMode" "elevation" "end" "exponent" "externalResourcesRequired"
    "fill" "filter" "filterRes" "filterUnits" "focusable" "format" "from" "fx" "fy"
    "g1" "g2" "glyphRef" "gradientTransform" "gradientUnits" "hanging" "ideographic"
    "in" "in2" "intercept" "k" "k1" "k2" "k3" "k4" "kernelMatrix" "kernelUnitLength"
    "kerning" "keyPoints" "keySplines" "keyTimes" "lengthAdjust" "limitingConeAngle"
    "local" "markerHeight" "markerUnits" "markerWidth" "mask" "maskContentUnits"
    "maskUnits" "mathematical" "mode" "numOctaves" "offset" "opacity" "operator"
    "order" "orient" "orientation" "origin" "overflow" "pathLength" "patternContentUnits"
    "patternTransform" "patternUnits" "points" "pointsAtX" "pointsAtY" "pointsAtZ"
    "preserveAlpha" "preserveAspectRatio" "primitiveUnits" "r" "radius" "refX" "refY"
    "repeatCount" "repeatDur" "requiredExtensions" "requiredFeatures" "restart"
    "result" "rotate" "rx" "ry" "scale" "seed" "slope" "spacing" "specularConstant"
    "specularExponent" "speed" "spreadMethod" "startOffset" "stdDeviation" "stemh"
    "stemv" "stitchTiles" "string" "stroke" "surfaceScale" "systemLanguage" "tableValues"
    "targetX" "targetY" "textLength" "to" "transform" "u1" "u2" "unicode" "values"
    "version" "viewBox" "viewTarget" "visibility" "widths" "x" "x1" "x2" "xChannelSelector"
    "xmlns" "y" "y1" "y2" "yChannelSelector" "z" "zoomAndPan" "arabicForm" "baselineShift"
    "capHeight" "clipPath" "clipRule" "colorInterpolation" "colorInterpolationFilters"
    "colorProfile" "colorRendering" "dominantBaseline" "enableBackground" "fillOpacity"
    "fillRule" "floodColor" "floodOpacity" "fontFamily" "fontSize" "fontSizeAdjust"
    "fontStretch" "fontStyle" "fontVariant" "fontWeight" "glyphName" "glyphOrientationHorizontal"
    "glyphOrientationVertical" "horizAdvX" "horizOriginX" "imageRendering" "letterSpacing"
    "lightingColor" "markerEnd" "markerMid" "markerStart" "overlinePosition" "overlineThickness"
    "paintOrder" "panose1" "pointerEvents" "renderingIntent" "shapeRendering" "stopColor"
    "stopOpacity" "strikethroughPosition" "strikethroughThickness" "strokeDasharray"
    "strokeDashoffset" "strokeLinecap" "strokeLinejoin" "strokeMiterlimit" "strokeOpacity"
    "strokeWidth" "textAnchor" "textDecoration" "textRendering" "underlinePosition"
    "underlineThickness" "unicodeBidi" "unicodeRange" "unitsPerEm" "vAlphabetic"
    "vHanging" "vIdeographic" "vMathematical" "vectorEffect" "vertAdvY" "vertOriginX"
    "vertOriginY" "wordSpacing" "writingMode" "xHeight"

    "xlinkActuate" "xlinkArcrole" "xlinkHref" "xlinkRole" "xlinkShow" "xlinkTitle"
    "xlinkType" "xmlBase" "xmlnsXlink" "xmlLang" "xmlSpace"

    ;; Non-standard Properties
    "autoCapitalize" "autoCorrect" "autoSave" "color" "itemProp" "itemScope"
    "itemType" "itemID" "itemRef" "results" "security" "unselectable"

    ;; Special case
    "data-reactid" "data-reactroot"})

(def no-suffix
  #{"animationIterationCount" "boxFlex" "boxFlexGroup" "boxOrdinalGroup"
    "columnCount" "fillOpacity" "flex" "flexGrow" "flexPositive" "flexShrink"
    "flexNegative" "flexOrder" "fontWeight" "lineClamp" "lineHeight" "opacity"
    "order" "orphans" "stopOpacity" "strokeDashoffset" "strokeOpacity"
    "strokeWidth" "tabSize" "widows" "zIndex" "zoom"})

(def lower-case-attrs
  #{"accessKey" "allowFullScreen" "allowTransparency" "as" "autoComplete"
    "autoFocus" "autoPlay" "contentEditable" "contextMenu" "crossOrigin"
    "cellPadding" "cellSpacing" "charSet" "classID" "colSpan" "dateTime"
    "encType" "formAction" "formEncType" "formMethod" "formNoValidate"
    "formTarget" "frameBorder" "hrefLang" "inputMode" "keyParams"
    "keyType" "marginHeight" "marginWidth" "maxLength" "mediaGroup"
    "minLength" "noValidate" "playsInline" "radioGroup" "readOnly" "rowSpan"
    "spellCheck" "srcDoc" "srcLang" "srcSet" "tabIndex" "useMap"
    "autoCapitalize" "autoCorrect" "autoSave" "itemProp" "itemScope"
    "itemType" "itemID" "itemRef"})

(def kebab-case-attrs
  #{"acceptCharset" "httpEquiv" "accentHeight" "alignmentBaseline" "arabicForm"
    "baselineShift" "capHeight" "clipPath" "clipRule" "colorInterpolation"
    "colorInterpolationFilters" "colorProfile" "colorRendering" "dominantBaseline"
    "enableBackground" "fillOpacity" "fillRule" "floodColor" "floodOpacity"
    "fontFamily" "fontSize" "fontSizeAdjust" "fontStretch" "fontStyle"
    "fontVariant" "fontWeight" "glyphName" "glyphOrientationHorizontal"
    "glyphOrientationVertical" "horizAdvX" "horizOriginX" "imageRendering"
    "letterSpacing" "lightingColor" "markerEnd" "markerMid" "markerStart"
    "overlinePosition" "overlineThickness" "paintOrder" "panose1" "pointerEvents"
    "renderingIntent" "shapeRendering" "stopColor" "stopOpacity" "strikethroughPosition"
    "strikethroughThickness" "strokeDasharray" "strokeDashoffset" "strokeLinecap"
    "strokeLinejoin" "strokeMiterlimit" "strokeOpacity" "strokeWidth" "textAnchor"
    "textDecoration" "textRendering" "underlinePosition" "underlineThickness"
    "unicodeBidi" "unicodeRange" "unitsPerEm" "vAlphabetic" "vHanging" "vIdeographic"
    "vMathematical" "vectorEffect" "vertAdvY" "vertOriginX" "vertOriginY" "wordSpacing"
    "writingMode" "xHeight"})

(def colon-between-attrs
  #{"xlinkActuate" "xlinkArcrole" "xlinkHref" "xlinkRole" "xlinkShow" "xlinkTitle"
    "xlinkType" "xmlBase" "xmlnsXlink" "xmlLang" "xmlSpace"})

(declare render-element!)

(defn append!
  ([sb s0] (.append sb s0))
  ([sb s0 s1]
   (.append sb s0)
   (.append sb s1))
  ([sb s0 s1 s2]
   (.append sb s0)
   (.append sb s1)
   (.append sb s2))
  ([sb s0 s1 s2 s3]
   (.append sb s0)
   (.append sb s1)
   (.append sb s2)
   (.append sb s3))
  ([sb s0 s1 s2 s3 s4]
   (.append sb s0)
   (.append sb s1)
   (.append sb s2)
   (.append sb s3)
   (.append sb s4))
  ([sb s0 s1 s2 s3 s4 & rest]
   (.append sb s0)
   (.append sb s1)
   (.append sb s2)
   (.append sb s3)
   (.append sb s4)
   (doseq [s rest]
     (.append sb s))))

(defn- string-builder []
  #?(:clj (StringBuilder.) :cljs (StringBuffer.)))

(defn escape-html ^String [^String s]
  (let [len (count s)]
    (loop [sb nil
           i (int 0)]
      (if (< i len)
        (let [char (.charAt s i)
              repl (case char
                     \& "&amp;"
                     \< "&lt;"
                     \> "&gt;"
                     \" "&quot;"
                     \' "&#x27;"
                     nil)]
          (if (nil? repl)
            (if (nil? sb)
              (recur nil (inc i))
              (recur (doto sb
                       (.append char))
                     (inc i)))
            (if (nil? sb)
              (recur (doto (string-builder)
                       (.append s 0 i)
                       (.append repl))
                     (inc i))
              (recur (doto sb
                       (.append repl))
                     (inc i)))))
        (if (nil? sb) s (str sb))))))

(defrecord Element [tag attrs react-key children]
  p/IReactDOMElement
  (-render-to-string [this react-id sb]
    (render-element! this react-id sb)))

(defrecord Text [s]
  p/IReactDOMElement
  (-render-to-string [this react-id sb]
    (assert (string? s))
    (append! sb (escape-html s))))

(defrecord ReactText [text]
  p/IReactDOMElement
  (-render-to-string [this react-id sb]
    (assert (string? text))
    (append! sb "<!-- react-text: " @react-id " -->" (escape-html text) "<!-- /react-text -->")
    (vswap! react-id inc)))

(defrecord ReactEmpty []
  p/IReactDOMElement
  (-render-to-string [this react-id sb]
    (append! sb "<!-- react-empty: " @react-id " -->")
    (vswap! react-id inc)))

(defn text-node
  "HTML text node"
  [s]
  (map->Text {:s s}))

(defn react-text-node
  "HTML text node"
  [s]
  (map->ReactText {:text s}))

(defn- react-empty-node []
  (map->ReactEmpty {}))

(defn- render-component [c]
  (if (or (nil? c)
          (instance? sablono.protocol.IReactDOMElement c)
          (satisfies? p/IReactDOMElement c))
    c
    (recur (p/-render c))))

(defn element
  "Creates a dom node."
  [{:keys [tag attrs react-key children] :as elem}]
  (assert (name tag))
  (assert (or (nil? attrs) (map? attrs)) (str "Invalid element attributes:" elem))
  (let [children (flatten children)
        child-node-count (count children)
        children (reduce
                  (fn [res c]
                    (let [c' (cond
                               (or (instance? sablono.protocol.IReactDOMElement c)
                                   (satisfies? p/IReactDOMElement c))
                               c

                               (or (instance? sablono.protocol.IReactComponent c)
                                   (satisfies? p/IReactComponent c))
                               (let [rendered (if-let [element (render-component c)]
                                                element
                                                (react-empty-node))]
                                 (assoc rendered :react-key
                                        (some-> (:props c) :omcljs$reactKey)))

                               (or (string? c) (number? c))
                               (let [c (cond-> c (number? c) str)]
                                 (if (> child-node-count 1)
                                   (react-text-node c)
                                   (text-node c)))

                               (nil? c) nil

                               :else
                               (throw (ex-info (str "Invalid child element: " c)
                                               {:element elem})))]
                      (cond-> res
                        (some? c') (conj c'))))
                  [] children)]
    (map->Element {:tag (name tag)
                   :attrs attrs
                   :react-key react-key
                   :children children})))

(defn camel->other-case [^String sep]
  (fn ^String [^String s]
    (-> s
        (str/replace #"([A-Z0-9])" (str sep "$1"))
        str/lower-case)))

(def camel->kebab-case
  (camel->other-case "-"))

(def camel->colon-between
  (camel->other-case ":"))

(defn coerce-attr-key ^String [^String k]
  (cond
    (contains? lower-case-attrs k) (str/lower-case k)
    (contains? kebab-case-attrs k) (camel->kebab-case k)
    ;; special cases
    (= k "className") "class"
    (= k "htmlFor") "for"
    (contains? colon-between-attrs k) (camel->colon-between k)
    :else k))

(defn render-xml-attribute! [sb name value]
  (let [name (coerce-attr-key (clojure.core/name name))]
    (append! sb " " name "=\""
             (cond-> value
               (string? value) escape-html) "\"")))

(defn normalize-styles! [sb styles]
  (letfn [(coerce-value [k v]
            (cond-> v
              (string? v)
              escape-html
              (and (number? v)
                   (not (contains? no-suffix k))
                   (pos? v))
              (str "px")))]
    (run! (fn [[k v]]
            (let [k (name k)]
              (append! sb (camel->kebab-case k) ":" (coerce-value k v) ";")))
          styles)))

(defn render-styles! [sb styles]
  (when-not (empty? styles)
    (append! sb " style=\"")
    (normalize-styles! sb styles)
    (append! sb "\"")))

(defn render-attribute! [sb [key value]]
  (cond
    (or (fn? value)
        (not value))
    nil

    (identical? key :style)
    (render-styles! sb value)

    ;; TODO: not sure if we want to limit values to strings/numbers - António
    (and (or (contains? supported-attrs (name key))
             (.startsWith (name key) "data-"))
         (or (true? value) (string? value) (number? value)))
    (if (true? value)
      (append! sb " " (coerce-attr-key (name key)))
      (render-xml-attribute! sb key value))

    :else nil))

;; some props assigned first in input and option. see:
;; https://github.com/facebook/react/blob/680685/src/renderers/dom/client/wrappers/ReactDOMOption.js#L108
;; https://github.com/facebook/react/blob/680685/src/renderers/dom/client/wrappers/ReactDOMInput.js#L63

(defn render-attr-map! [sb tag attrs]
  (letfn [(sorter [order]
            (fn [[k _]]
              (get order k (->> (vals order)
                                (apply max)
                                inc))))]
    (let [attrs (cond->> attrs
                  (= tag "input") (sort-by (sorter {:type 0 :step 1
                                                    :min 2 :max 3}))
                  (= tag "option") (sort-by (sorter {:selected 0})))]
      (run! (partial render-attribute! sb) attrs))))

(def ^{:doc "A list of elements that must be rendered without a closing tag."
       :private true}
  void-tags
  #{"area" "base" "br" "col" "command" "embed" "hr" "img" "input" "keygen" "link"
    "meta" "param" "source" "track" "wbr"})

(defn render-unescaped-html! [sb m]
  (if-not (contains? m :__html)
    (throw (ex-info "`props.dangerouslySetInnerHTML` must be in the form `{:__html ...}`" {:element m})))
  (when-let [html (:__html m)]
    (append! sb html)))

(defn container-tag?
  "Returns true if the tag has content or is not a void tag. In non-HTML modes,
      all contentless tags are assumed to be void tags."
  [tag content]
  (or content (and (not (void-tags tag)))))

(defn render-element!
  "Render a tag vector as a HTML element string."
  [{:keys [tag attrs children]} react-id sb]
  (append! sb "<" tag)
  (render-attr-map! sb tag attrs)
  (let [react-id-val @react-id]
    (when (= react-id-val 1)
      (append! sb " data-reactroot=\"\""))
    (append! sb " data-reactid=\"" react-id-val "\"")
    (vswap! react-id inc))
  (if (container-tag? tag (seq children))
    (do
      (append! sb ">")
      (if-let [html-map (:dangerouslySetInnerHTML attrs)]
        (render-unescaped-html! sb html-map)
        (run! #(p/-render-to-string % react-id sb) children))
      (append! sb "</" tag ">"))
    (append! sb "/>")))

(def key-escape-lookup
  {"=" "=0"
   ":" "=2"})

;; preserves testability without having to compute checksums
(defn- render-to-str* [x]
  {:pre [(or (instance? sablono.protocol.IReactComponent x)
             (instance? sablono.protocol.IReactDOMElement x)
             (satisfies? p/IReactComponent x)
             (satisfies? p/IReactDOMElement x))]}
  (let [element (if-let [element (cond-> x
                                   (or (instance? sablono.protocol.IReactComponent x)
                                       (satisfies? p/IReactComponent x))
                                   render-component)]
                  element
                  (react-empty-node))
        sb (string-builder)]
    (p/-render-to-string element (volatile! 1) sb)
    sb))

(defn render-to-str ^String [x]
  (let [sb (render-to-str* x)]
    (assign-react-checksum sb)
    (str sb)))

(defn node
  "Returns the dom node associated with a component's React ref."
  ([component]
   {:pre [(or (instance? sablono.protocol.IReactComponent component)
              (satisfies? p/IReactComponent component))]}
   (p/-render component))
  ([component name]
   {:pre [(or (instance? sablono.protocol.IReactComponent component)
              (satisfies? p/IReactComponent component))]}
   (some-> @(:refs component) (get name) p/-render)))

(defn create-element
  "Create a DOM element for which there exists no corresponding function.
      Useful to create DOM elements not included in React.DOM. Equivalent
      to calling `js/React.createElement`"
  ([tag]
   (create-element tag nil))
  ([tag opts & children]
   (element {:tag tag
             :attrs (dissoc opts :ref :key)
             :react-key (:key opts)
             :children children})))
