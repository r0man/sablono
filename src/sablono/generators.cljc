(ns sablono.generators
  (:require [clojure.pprint :refer [pprint]]
            [clojure.test.check.generators :as gen]))

(def html-types
  (gen/elements #{:div :a}))

(def html-attribute-keys
  (gen/elements
   #{:spread-method :auto-complete :tab-index :attribute-name :mask-units :xlink-role :gradient-transform :radio-group :content-style-type :max-length :xmlns-xlink :stitch-tiles :marker-width :target-x :date-time :required-extensions :auto-capitalize :preserve-aspect-ratio :ref-x :min-length :pattern-transform :x-channel-selector :cell-padding :xlink-title :clip-path-units :frame-border :default-checked :content-editable :repeat-dur :table-values :margin-width :access-key :marker-units :specular-exponent :xlink-href :item-id :form-action :surface-scale :ref-y :default-value :auto-save :href-lang :auto-reverse :points-at-y :num-octaves :key-type :form-target :xml-lang :xlink-type :y-channel-selector :view-box :form-no-validate :points-at-z :preserve-alpha :length-adjust :xml-base :use-map :kernel-matrix :allow-full-screen :glyph-ref :points-at-x :zoom-and-pan :src-set :external-resources-required :filter-res :enc-type :base-profile :item-type :http-equiv :edge-mode :item-prop :kernel-unit-length :referrer-policy :system-language #_:auto-play :class-id :key-params :view-target :item-ref :key-times :src-lang :media-group :accept-charset :std-deviation :no-validate :char-set :content-script-type :xml-space :context-menu :item-scope :key-points :cross-origin :start-offset :col-span :xlink-arcrole :src-doc :path-length :diffuse-constant :target-y :cell-spacing :spell-check :key-splines :pattern-content-units :xlink-show :read-only :allow-reorder :form-method :primitive-units :allow-transparency :calc-mode :required-features :attribute-type :marker-height :margin-height :base-frequency :input-mode :filter-units :gradient-units :limiting-cone-angle :specular-constant :pattern-units :xlink-actuate :form-enc-type :auto-correct :mask-content-units :repeat-count :row-span}))

(def html-attributes
  (gen/map html-attribute-keys gen/string-alphanumeric))

(defn element-with-attributes
  [child-gen]
  (gen/tuple html-types html-attributes child-gen))

(defn element-without-attributes
  [child-gen]
  (gen/tuple html-types child-gen))

(defn container
  [child-gen]
  (gen/one-of [(element-with-attributes child-gen)
               (element-without-attributes child-gen)]))

(def children
  (gen/one-of [gen/string-alphanumeric gen/int]))

(def elements
  (gen/recursive-gen container children))

(comment
  (pprint (gen/sample elements 10))
  (pprint (last (gen/sample elements 20))))
