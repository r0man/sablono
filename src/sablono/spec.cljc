(ns sablono.spec
  (:require [clojure.spec :as s]
            [clojure.spec.gen :as gen]))

(s/def ::html-type
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
    :br
    :button
    :canvas
    :caption
    :circle
    :cite
    :clipPath
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
    :linearGradient
    :link
    :main
    :map
    :mark
    :mask
    :menu
    :menuitem
    :meta
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
    :radialGradient
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
    :use
    :var
    :video
    :wbr})

;; (s/def ::html-sequence
;;   (s/cat
;;    :type ::html-type
;;    :attrs ::html-attributes
;;    :children (s/spec (s/* ::html-content))))

;; (defn html-element?
;;   "Returns true if `x` is an HTML element."
;;   [x]
;;   (and (vector? x) (keyword? (first x))))

;; (s/def ::html-element
;;   (s/with-gen html-element?
;;     #(gen/fmap vec (s/gen ::html-sequence))))

;; (s/def ::html-content
;;   (s/or
;;    :string string?
;;    :number number?
;;    :element ::html-element))


(s/def ::html-type
  #{:div})

(s/def ::html-attributes map?)

(s/def ::html-children
  (s/coll-of (s/spec (s/* ::html-element))))

(s/def ::html-element-with-attributes
  (s/tuple ::html-type ::html-attributes ::html-children))

(s/def ::html-element-without-attributes
  (s/tuple ::html-type ::html-children))

(s/def ::html-element
  (s/or
   :with-attributes ::html-element-with-attributes
   :without-attributes ::html-element-without-attributes))

;; http://dev.clojure.org/jira/browse/CLJ-1980

(s/def ::html-element
  (s/with-gen
    (s/or
     :with-attributes ::html-element-with-attributes
     :without-attributes ::html-element-without-attributes)
    #(gen/one-of
      [(s/gen ::html-element-with-attributes)
       (s/gen ::html-element-without-attributes)])))

(comment

  (prn (gen/sample (s/gen ::html-element) 1))
  ;;=> ([:div {} [() () () () () () () ()]])

  (prn (gen/sample (s/gen ::html-element) 2))
  ;; Unhandled clojure.lang.ExceptionInfo
  ;; Unable to construct gen at: [:without-attributes 1 :without-attributes 1 :without-attributes 1 :with-attributes 2]
  ;; for: :sablono.spec/html-element #:clojure.spec{:path [:without-attributes 1 :without-attributes 1 :without-attributes 1 :with-attributes 2], :form :sablono.spec/html-element, :failure :no-gen}

  (prn (gen/sample (s/gen ::html-element-with-attributes) 2))
  (prn (gen/sample (s/gen ::html-element-without-attributes) 2)))

(comment
  (gen/sample (s/gen ::html-element) 10)
  (gen/fmap vec (s/gen ::html-sequence))
  (gen/sample (s/gen ::html-content))
  (gen/sample (gen/fmap vec (s/gen ::html-sequence)) 10)
  (gen/sample (s/gen ::html-sequence) 2)
  (gen/sample (s/gen ::html-content) 5)

  (gen/sample (s/gen ::html-element) 5)
  (s/exercise ::html-element 1))
