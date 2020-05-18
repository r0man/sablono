(ns sablono.specs
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.test.check.generators :refer [recursive-gen]]))

(def void-tags
  #{:area
    :base
    :br
    :col
    :command
    :embed
    :hr
    :img
    :input
    :keygen
    :link
    :meta
    :menuitem
    :param
    :source
    :track
    :wbr})

(def tags
  #{:a
    :abbr
    :address
    :area
    :article
    :aside
    :audio
    :b
    :base
    :bdi
    :bdo
    :big
    :blockquote
    :body
    ;; :br
    :button
    :canvas
    :caption
    :circle
    :cite
    :clippath
    :code
    :col
    :colgroup
    :data
    :datalist
    :dd
    :defs
    :del
    :details
    :dfn
    :dialog
    :div
    :dl
    :dt
    :ellipse
    :em
    :embed
    :fieldset
    :figcaption
    :figure
    :footer
    :form
    :g
    :h1
    :h2
    :h3
    :h4
    :h5
    :h6
    :head
    :header
    :hr
    :html
    :i
    :iframe
    :img
    :ins
    :kbd
    :keygen
    :label
    :legend
    :li
    :line
    :lineargradient
    :link
    :main
    :map
    :mark
    :mask
    :menu
    :menuitem
    ;; :meta
    :meter
    :nav
    :noscript
    :object
    :ol
    :optgroup
    :output
    :p
    :param
    :path
    :pattern
    :picture
    :polygon
    :polyline
    :pre
    :progress
    :q
    :radialgradient
    :rect
    :rp
    :rt
    :ruby
    :s
    :samp
    :script
    :section
    :small
    :source
    :span
    :stop
    :strong
    :style
    :sub
    :summary
    :sup
    :svg
    :table
    :tbody
    :td
    :text
    :tfoot
    :th
    :thead
    :time
    :title
    :tr
    :track
    :tspan
    :u
    :ul
    :var
    :video
    :wbr})

(s/def ::attribute
  #{#_:auto-play
    :accept-charset
    :access-key
    :allow-full-screen
    :allow-reorder
    :allow-transparency
    :attribute-name
    :attribute-type
    :auto-capitalize
    :auto-complete
    :auto-correct
    :auto-reverse
    :auto-save
    :base-frequency
    :base-profile
    :calc-mode
    :cell-padding
    :cell-spacing
    :char-set
    :class-id
    :clip-path-units
    :col-span
    :content-editable
    :content-script-type
    :content-style-type
    :context-menu
    :cross-origin
    :date-time
    :default-checked
    :default-value
    :diffuse-constant
    :edge-mode
    :enc-type
    :external-resources-required
    :filter-res
    :filter-units
    :form-action
    :form-enc-type
    :form-method
    :form-no-validate
    :form-target
    :frame-border
    :glyph-ref
    :gradient-transform
    :gradient-units
    :href-lang
    :http-equiv
    :input-mode
    :item-id
    :item-prop
    :item-ref
    :item-scope
    :item-type
    :kernel-matrix
    :kernel-unit-length
    :key-params
    :key-points
    :key-splines
    :key-times
    :key-type
    :length-adjust
    :limiting-cone-angle
    :margin-height
    :margin-width
    :marker-height
    :marker-units
    :marker-width
    :mask-content-units
    :mask-units
    :max-length
    :media-group
    :min-length
    :no-validate
    :num-octaves
    :path-length
    :pattern-content-units
    :pattern-transform
    :pattern-units
    :points-at-x
    :points-at-y
    :points-at-z
    :preserve-alpha
    :preserve-aspect-ratio
    :primitive-units
    :radio-group
    :read-only
    :ref-x
    :ref-y
    :referrer-policy
    :repeat-count
    :repeat-dur
    :required-extensions
    :required-features
    :row-span
    :specular-constant
    :specular-exponent
    :spell-check
    :spread-method
    :src-doc
    :src-lang
    :src-set
    :start-offset
    :std-deviation
    :stitch-tiles
    :surface-scale
    :system-language
    :tab-index
    :table-values
    :target-x
    :target-y
    :use-map
    :view-box
    :view-target
    :x-channel-selector
    :xlink-actuate
    :xlink-arcrole
    :xlink-href
    :xlink-role
    :xlink-show
    :xlink-title
    :xlink-type
    :xml-base
    :xml-lang
    :xml-space
    :xmlns-xlink
    :y-channel-selector
    :zoom-and-pan})

(s/def ::void-tag void-tags)

;; TODO: Test all tags. Void tags can't be parsed with the Java SAX parser.
(s/def ::tag (set/difference tags void-tags))

(s/def ::class-name
  (s/and string? not-empty))

(s/def ::attributes
  (s/map-of ::attribute string?))

(s/def ::simple-child
  (s/or :integer int?
        :string string?))

(s/def ::child
  (s/or :complex ::element
        :simple ::simple-child))

(s/def ::children (s/* ::child))

(def element-gen
  "A generator that produces HTML elements."
  (recursive-gen
   #(gen/hash-map
     :tag (s/gen ::tag)
     :attributes (s/gen ::attributes)
     :children %)
   (gen/list (s/gen ::simple-child))))

(s/def ::element
  (s/with-gen (s/keys :req-un [::tag ::attributes ::children])
    (constantly element-gen)))
