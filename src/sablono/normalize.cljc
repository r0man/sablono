(ns sablono.normalize
  (:refer-clojure :exclude [class])
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [sablono.util :as util]))

(def attribute-mapping
  { ;; special cases
   :default-checked "checked"
   :default-value "value"

   ;; https://github.com/facebook/react/blob/master/src/renderers/dom/shared/HTMLDOMPropertyConfig.js
   :accept-charset "accept-charset"
   :access-key "accesskey"
   :allow-full-screen "allowfullscreen"
   :allow-transparency "allowtransparency"
   :auto-complete "autocomplete"
   :auto-play "autoplay"
   :cell-padding "cellpadding"
   :cell-spacing "cellspacing"
   :char-set "charset"
   :class-id "classid"
   :col-span "colspan"
   :content-editable "contenteditable"
   :context-menu "contextmenu"
   :cross-origin "crossorigin"
   :date-time "datetime"
   :enc-type "enctype"
   :form-action "formaction"
   :form-enc-type "formenctype"
   :form-method "formmethod"
   :form-no-validate "formnovalidate"
   :form-target "formtarget"
   :frame-border "frameborder"
   :href-lang "hreflang"
   :http-equiv "http-equiv"
   :input-mode "inputmode"
   :key-params "keyparams"
   :key-type "keytype"
   :margin-height "marginheight"
   :margin-width "marginwidth"
   :max-length "maxlength"
   :media-group "mediagroup"
   :min-length "minlength"
   :no-validate "novalidate"
   :radio-group "radiogroup"
   :referrer-policy "referrerpolicy"
   :read-only "readonly"
   :row-span "rowspan"
   :spell-check "spellcheck"
   :src-doc "srcdoc"
   :src-lang "srclang"
   :src-set "srcset"
   :tab-index "tabindex"
   :use-map "usemap"
   :auto-capitalize "autocapitalize"
   :auto-correct "autocorrect"
   :auto-save "autosave"
   :item-prop "itemprop"
   :item-scope "itemscope"
   :item-type "itemtype"
   :item-id "itemID"
   :item-ref "itemref"

   ;; https://github.com/facebook/react/blob/master/src/renderers/dom/shared/SVGDOMPropertyConfig.js
   :allow-reorder "allowReorder"
   :attribute-name "attributeName"
   :attribute-type "attributeType"
   :auto-reverse "autoReverse"
   :base-frequency "baseFrequency"
   :base-profile "baseProfile"
   :calc-mode "calcMode"
   :clip-path-units "clipPathUnits"
   :content-script-type "contentScriptType"
   :content-style-type "contentStyleType"
   :diffuse-constant "diffuseConstant"
   :edge-mode "edgeMode"
   :external-resources-required "externalResourcesRequired"
   :filter-res "filterRes"
   :filter-units "filterUnits"
   :glyph-ref "glyphRef"
   :gradient-transform "gradientTransform"
   :gradient-units "gradientUnits"
   :kernel-matrix "kernelMatrix"
   :kernel-unit-length "kernelUnitLength"
   :key-points "keyPoints"
   :key-splines "keySplines"
   :key-times "keyTimes"
   :length-adjust "lengthAdjust"
   :limiting-cone-angle "limitingConeAngle"
   :marker-height "markerHeight"
   :marker-units "markerUnits"
   :marker-width "markerWidth"
   :mask-content-units "maskContentUnits"
   :mask-units "maskUnits"
   :num-octaves "numOctaves"
   :path-length "pathLength"
   :pattern-content-units "patternContentUnits"
   :pattern-transform "patternTransform"
   :pattern-units "patternUnits"
   :points-at-x "pointsAtX"
   :points-at-y "pointsAtY"
   :points-at-z "pointsAtZ"
   :preserve-alpha "preserveAlpha"
   :preserve-aspect-ratio "preserveAspectRatio"
   :primitive-units "primitiveUnits"
   :ref-x "refX"
   :ref-y "refY"
   :repeat-count "repeatCount"
   :repeat-dur "repeatDur"
   :required-extensions "requiredExtensions"
   :required-features "requiredFeatures"
   :specular-constant "specularConstant"
   :specular-exponent "specularExponent"
   :spread-method "spreadMethod"
   :start-offset "startOffset"
   :std-deviation "stdDeviation"
   :stitch-tiles "stitchTiles"
   :surface-scale "surfaceScale"
   :system-language "systemLanguage"
   :table-values "tableValues"
   :target-x "targetX"
   :target-y "targetY"
   :view-box "viewBox"
   :view-target "viewTarget"
   :x-channel-selector "xChannelSelector"
   :xlink-actuate "xlink:actuate"
   :xlink-arcrole "xlink:arcrole"
   :xlink-href "xlink:href"
   :xlink-role "xlink:role"
   :xlink-show "xlink:show"
   :xlink-title "xlink:title"
   :xlink-type "xlink:type"
   :xml-base "xml:base"
   :xmlns-xlink "xmlns:xlink"
   :xml-lang "xml:lang"
   :xml-space "xml:space"
   :y-channel-selector "yChannelSelector"
   :zoom-and-pan "zoomAndPan"})

(defn html-to-dom-attrs [attributes]
  (->> (for [[k v] attributes]
         [(or (attribute-mapping k) (name k)) v])
       (into {})))

(defn compact-map
  "Removes all map entries where the value of the entry is empty."
  [m]
  (reduce
   (fn [m k]
     (let [v (get m k)]
       (if (empty? v)
         (dissoc m k) m)))
   m (keys m)))

(defn class-name
  [x]
  (cond
    (string? x) x
    (keyword? x) (name x)
    :else x))

(defn map-lookup?
  "Returns true if `x` is a map lookup form, otherwise false."
  [x]
  (and (list? x) (keyword? (first x))))

(defn class
  "Normalize `class` into a vector of classes."
  [class]
  (cond
    (nil? class)
    nil

    (map-lookup? class)
    [class]

    (list? class)
    (if (symbol? (first class))
      [class]
      (map class-name class))

    (symbol? class)
    [class]

    (string? class)
    [class]

    (keyword? class)
    [(class-name class)]

    (and (or (set? class)
             (sequential? class))
         (every? #(or (keyword? %)
                      (string? %))
                 class))
    (mapv class-name class)

    (and (or (set? class)
             (sequential? class)))
    (mapv class-name class)

    :else class))

(defn attributes
  "Normalize the `attrs` of an element."
  [attrs]
  (cond-> attrs
    (:class attrs)
    (update-in [:class] class)))

(defn merge-with-class
  "Like clojure.core/merge but concatenate :class entries."
  [& maps]
  (let [maps (map attributes maps)
        classes (map :class maps)
        classes (vec (apply concat classes))]
    (cond-> (apply merge maps)
      (not (empty? classes))
      (assoc :class classes))))

(defn strip-css
  "Strip the # and . characters from the beginning of `s`."
  [s] (if s (str/replace s #"^[.#]" "")))

(defn match-tag
  "Match `s` as a CSS tag and return a vector of tag name, CSS id and
  CSS classes."
  [s]
  (let [matches (re-seq #"[#.]?[^#.]+" (name s))
        [tag-name names]
        (cond (empty? matches)
              (throw (ex-info (str "Can't match CSS tag: " s) {:tag s}))
              (#{\# \.} (ffirst matches)) ;; shorthand for div
              ["div" matches]
              :default
              [(first matches) (rest matches)])]
    [tag-name
     (first (map strip-css (filter #(= \# (first %1)) names)))
     (vec (map strip-css (filter #(= \. (first %1)) names)))]))

(defn children
  "Normalize the children of a HTML element."
  [x]
  (->> (cond
         (string? x)
         (list x)
         (util/element? x)
         (list x)
         (and (list? x)
              (symbol? x))
         (list x)
         (list? x)
         x
         (and (sequential? x)
              (sequential? (first x))
              (not (string? (first x)))
              (not (util/element? (first x)))
              (= (count x) 1))
         (children (first x))
         (sequential? x)
         x
         :else (list x))
       (remove nil?)))

(defn element
  "Ensure an element vector is of the form [tag-name attrs content]."
  [[tag & content]]
  (when (not (or (keyword? tag) (symbol? tag) (string? tag)))
    (throw (ex-info (str tag " is not a valid element name.") {:tag tag :content content})))
  (let [[tag id class] (match-tag tag)
        tag-attrs (compact-map {:id id :class class})
        map-attrs (first content)]
    (if (map? map-attrs)
      [tag
       (merge-with-class tag-attrs map-attrs)
       (children (next content))]
      [tag
       (attributes tag-attrs)
       (children content)])))
