(ns sablono.render
  (:require [clojure.string :as str]
            [sablono.checksum :as chk]
            [sablono.normalize :as normalize])
  ;; (:import [clojure.lang IPersistentVector ISeq Named Numbers Ratio Keyword])
  (:import [goog.string StringBuffer]))

(defn nothing? [element]
  (and (vector? element)
       (= :rum/nothing (first element))))


(def ^:dynamic *select-value*)

(defn append!
  ([^StringBuffer sb s0]
   (.append sb s0))
  ([^StringBuffer sb s0 s1]
   (.append sb s0)
   (.append sb s1))
  ([^StringBuffer sb s0 s1 s2]
   (.append sb s0)
   (.append sb s1)
   (.append sb s2))
  ([^StringBuffer sb s0 s1 s2 s3]
   (.append sb s0)
   (.append sb s1)
   (.append sb s2)
   (.append sb s3))
  ([^StringBuffer sb s0 s1 s2 s3 s4]
   (.append sb s0)
   (.append sb s1)
   (.append sb s2)
   (.append sb s3)
   (.append sb s4)))

(defprotocol ToString
  (to-str [x] "Convert a value into a string."))

(extend-protocol ToString
  cljs.core.Keyword
  (to-str [k] (name k))
  ;; cljs.core.Ratio
  ;; (to-str [r] (str (float r)))
  string
  (to-str [s] s)
  object
  (to-str [x] (str x))
  nil
  (to-str [_] ""))

(def ^{:doc "A list of elements that must be rendered without a closing tag."
       :private true}
  void-tags
  #{"area" "base" "br" "col" "command" "embed" "hr" "img" "input" "keygen" "link"
    "meta" "param" "source" "track" "wbr"})

(defn get-value [attrs]
  (or (:value attrs)
      (:default-value attrs)))

(defn normalize-attr-key ^String [key]
  (or (normalize/attribute-mapping key)
      (name key)))

(defn escape-html [^String s]
  (let [len (count s)]
    (loop [^StringBuffer sb nil
           i                 (int 0)]
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
              (recur (doto (StringBuffer.)
                       (.append s 0 i)
                       (.append repl))
                     (inc i))
              (recur (doto sb
                       (.append repl))
                     (inc i)))))
        (if (nil? sb) s (str sb))))))

(defn parse-selector [s]
  (loop [matches (re-seq #"([#.])?([^#.]+)" (name s))
         tag     "div"
         id      nil
         classes nil]
    (if-let [[_ prefix val] (first matches)]
      (case prefix
        nil (recur (next matches) val id classes)
        "#" (recur (next matches) tag val classes)
        "." (recur (next matches) tag id (conj (or classes []) val)))
      [tag id classes])))

(defn normalize-element [[first second & rest]]
  (when-not (or (keyword? first)
                (symbol? first)
                (string? first))
    (throw (ex-info "Expected a keyword as a tag" { :tag first })))
  (let [[tag tag-id tag-classes] (parse-selector first)
        [attrs children] (if (or (map? second)
                                 (nil? second))
                           [second rest]
                           [nil    (cons second rest)])
        attrs-classes    (:class attrs)
        classes          (if (and tag-classes attrs-classes)
                           [tag-classes attrs-classes]
                           (or tag-classes attrs-classes))]
    [tag tag-id classes attrs children]))


;;; render attributes

;; https://github.com/facebook/react/blob/master/src/renderers/dom/shared/CSSProperty.js
(def unitless-css-props
  (into #{}
        (for [key ["animation-iteration-count" "box-flex" "box-flex-group" "box-ordinal-group" "column-count" "flex" "flex-grow" "flex-positive" "flex-shrink" "flex-negative" "flex-order" "grid-row" "grid-column" "font-weight" "line-clamp" "line-height" "opacity" "order" "orphans" "tab-size" "widows" "z-index" "zoom" "fill-opacity" "stop-opacity" "stroke-dashoffset" "stroke-opacity" "stroke-width"]
              prefix ["" "-webkit-" "-ms-" "-moz-" "-o-"]]
          (str prefix key))))

(defn normalize-css-key [k]
  (-> (to-str k)
      (str/replace #"[A-Z]" (fn [ch] (str "-" (str/lower-case ch))))
      (str/replace #"^ms-" "-ms-")))

(defn normalize-css-value [key value]
  (cond
    (contains? unitless-css-props key)
    (escape-html (to-str value))
    (number? value)
    (str value (when (not= 0 value) "px"))
    (and (string? value)
         (re-matches #"\s*\d+\s*" value))
    ;; (recur key (-> value str/trim Long/parseLong))
    (recur key (-> value str/trim js/parseInt))
    (and (string? value)
         (re-matches #"\s*\d+\.\d+\s*" value))
    ;; (recur key (-> value str/trim Double/parseDouble))
    (recur key (-> value str/trim js/parseFloat))
    :else
    (escape-html (to-str value))))

(defn render-style-kv! [sb empty? k v]
  (if v
    (do
      (when empty?
        (append! sb " style=\""))
      (let [key (normalize-css-key k)
            val (normalize-css-value key v)]
        (append! sb key ":" val ";"))
      false)
    empty?))


(defn render-style! [map sb]
  (let [empty? (reduce-kv (partial render-style-kv! sb) true map)]
    (when-not empty?
      (append! sb "\""))))


(defn render-class! [sb first? class]
  (cond
    (nil? class)
    first?
    (string? class)
    (do
      (when-not first?
        (append! sb " "))
      (append! sb class)
      false)
    (or (sequential? class)
        (set? class))
    (reduce #(render-class! sb %1 %2) first? class)
    :else
    (render-class! sb first? (to-str class))))


(defn render-classes! [classes sb]
  (when classes
    (append! sb " class=\"")
    (render-class! sb true classes)
    (append! sb "\"")))


(defn render-attr! [tag key value sb]
  (let [attr (normalize-attr-key key)]
    (cond
      (= "type" attr)  :nop ;; rendered manually in render-element! before id
      (= "style" attr) (render-style! value sb)
      (= "key" attr)   :nop
      (= "ref" attr)   :nop
      (= "class" attr) :nop
      (and (= "value" attr)
           (or (= "select" tag)
               (= "textarea" tag))) :nop
      (not value)      :nop
      (true? value)    (append! sb " " attr "=\"\"")
      (.startsWith attr "on")            :nop
      (= "dangerouslySetInnerHTML" attr) :nop
      :else            (append! sb " " attr "=\"" (to-str value) "\""))))


(defn render-attrs! [tag attrs sb]
  (reduce-kv (fn [_ k v] (render-attr! tag k v sb)) nil attrs))


;;; render html


(defprotocol HtmlRenderer
  (-render-html [this parent *key sb]
    "Turn a Clojure data type into a string of HTML with react ids."))


(defn render-inner-html! [attrs children sb]
  (when-let [inner-html (:dangerouslySetInnerHTML attrs)]
    (when-not (empty? children)
      (throw (ex-info "Invariant Violation: Can only set one of `children` or `props.dangerouslySetInnerHTML`." {})))
    (when-not (:__html inner-html)
      (throw (ex-info "Invariant Violation: `props.dangerouslySetInnerHTML` must be in the form `{__html: ...}`. Please visit https://fb.me/react-invariant-dangerously-set-inner-html for more information." {})))
    (append! sb (:__html inner-html))
    true))


(defn render-textarea-value! [tag attrs sb]
  (when (= tag "textarea")
    (when-some [value (get-value attrs)]
      (append! sb (escape-html value))
      true)))


(defn render-content! [tag attrs children *key sb]
  (if (and (nil? children)
           (contains? void-tags tag))
    (append! sb "/>")
    (do
      (append! sb ">")
      (or (render-textarea-value! tag attrs sb)
          (render-inner-html! attrs children sb)
          (doseq [element children]
            (-render-html element children *key sb)))
      (append! sb "</" tag ">"))))


(defn render-element!
  "Render an element vector as a HTML element."
  [element *key sb]
  (if (nothing? element)
    (when *key
      (let [key @*key]
        (vswap! *key inc)
        (append! sb "<!-- react-empty: " key " -->")))
    (let [[tag id classes attrs children] (normalize-element element)]
      (append! sb "<" tag)

      (when-some [type (:type attrs)]
        (append! sb " type=\"" type "\""))

      (when (and (= "option" tag)
                 (= (get-value attrs) *select-value*))
        (append! sb " selected=\"\""))

      (when id
        (append! sb " id=\"" id "\""))

      (render-attrs! tag attrs sb)

      (render-classes! classes sb)

      (when *key
        (when (== @*key 1)
          (append! sb " data-reactroot=\"\""))

        (append! sb " data-reactid=\"" @*key "\"")
        (vswap! *key inc))

      (if (= "select" tag)
        (binding [*select-value* (get-value attrs)]
          (render-content! tag attrs children *key sb))
        (render-content! tag attrs children *key sb)))))


(extend-protocol HtmlRenderer
  ;; PersistentVector
  cljs.core.PersistentVector
  (-render-html [this parent *key sb]
    (render-element! this *key sb))

  ;; ISeq
  cljs.core.ISeq
  (-render-html [this parent *key sb]
    (doseq [element this]
      (-render-html element parent *key sb)))

  ;; Named
  cljs.core.INamed
  (-render-html [this parent *key sb]
    (append! sb (name this)))

  number
  (-render-html [this parent *key sb]
    (-render-html (str this) parent *key sb))
  
  ;; String
  string
  (-render-html [this parent *key sb]
    (if (and *key
             (> (count parent) 1))
      (let [key @*key]
        (vswap! *key inc)
        (append! sb "<!-- react-text: " key " -->" (escape-html this) "<!-- /react-text -->"))
      (append! sb (escape-html this))))

  object
  (-render-html [this parent *key sb]
    (-render-html (str this) parent *key sb))

  nil
  (-render-html [this parent *key sb]
    :nop))

(defn render-html
  ([src] (render-html src nil))
  ([src opts]
   (let [sb (StringBuffer.)]
     (-render-html src nil (volatile! 1) sb)
     ;; (when-not (nothing? src)
     ;;   (.insert sb (.indexOf (.toString sb) ">") (str " data-react-checksum=\"" (chk/adler32 sb) "\"")))
     (if (nothing? src)
       (str sb)
       (str/replace-first (str sb) ">" (str " data-react-checksum=\"" (chk/adler32 sb) "\">"))))))


(defn render-static-markup [src]
  (let [sb (StringBuffer.)]
    (-render-html src nil nil sb)
    (str sb)))
