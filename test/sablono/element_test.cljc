(ns sablono.element-test
  (:require #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [are is]])
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop #?@(:cljs [:include-macros true])]
            [clojure.test.check.clojure-test #?(:clj :refer :cljs :refer-macros) [defspec]]
            [sablono.element :as element]
            [clojure.string :as str]
            [sablono.util :as util]))


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
    tspan
    use])

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

(def gen-types
  "A generator that produces HTML element type names."
  (gen/fmap str (gen/elements tags)))

(def gen-attribute-keys
  "A generator that produces HTML attribute keywords."
  (gen/fmap keyword (gen/fmap util/hyphenate (gen/elements supported-attrs))))

(def gen-attribute-vals
  "A generator that produces HTML attribute values."
  (gen/one-of [gen/int gen/string-alphanumeric]))

(def gen-attributes
  (gen/fmap str (gen/elements tags)))

(gen/sample gen-attribute-keys)

(gen/sample gen-attribute-vals)

(gen/sample gen-types)

(def gen-element
  (gen/fmap
   (fn [[type attributes children]]
     (element/create type attributes children))
   (gen/tuple
    gen-types
    (gen/elements [nil {}])
    (gen/return []))))

(def gen-children
  (gen/vector gen-types))

(defspec test-type
  (prop/for-all
   [type gen-types]
   (= (element/type (element/create type nil nil)) type)))

(defspec test-children
  (prop/for-all
   [type gen-types
    children gen-children]
   (= (element/children (element/create type nil children)) children)))
