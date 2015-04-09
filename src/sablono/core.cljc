(ns sablono.core
  #?(:cljs (:require-macros [sablono.core :refer [defelem gen-input-fields]]))
  (:require [clojure.string :refer [upper-case]]
            [clojure.walk :refer [postwalk-replace]]
            [sablono.util :refer [as-str to-uri]]
            [sablono.interpreter :as interpreter]
            #?(:clj [sablono.compiler :as compiler])
            #?(:cljs [goog.dom :as dom])
            #?(:cljs cljsjs.react)))

#?(:clj
   (defmacro html
  "Render Clojure data structures via Facebook's React."
  [options & content]
  (apply sablono.compiler/compile-html options content)))

#?(:clj
   (defmacro html-expand
  "Returns the expanded HTML generation forms."
  [& forms]
  `(macroexpand `(html ~~@forms))))

#?(:clj
   (defmacro defhtml
  "Define a function, but wrap its output in an implicit html macro."
  [name & fdecl]
  (let [[fhead fbody] (split-with #(not (or (list? %) (vector? %))) fdecl)
        wrap-html (fn [[args & body]] `(~args (html ~@body)))]
    `(defn ~name
       ~@fhead
       ~@(if (vector? (first fbody))
           (wrap-html fbody)
           (map wrap-html fbody))))))
#?(:clj
   (defmacro with-base-url
  "Sets a base URL that will be prepended onto relative URIs. Note that for this
  to work correctly, it needs to be placed outside the html macro."
  [base-url & body]
  `(binding [sablono.util/*base-url* ~base-url]
     ~@body)))

(defn wrap-attrs
  "Add an optional attribute argument to a function that returns a element vector."
  [func]
  (fn [& args]
    (if (map? (first args))
      (let [[tag & body] (apply func (rest args))]
        (if (map? (first body))
          (apply vector tag (merge (first body) (first args)) (rest body))
          (apply vector tag (first args) body)))
      (apply func args))))

(defn- update-arglists [arglists]
  (for [args arglists]
    (vec (cons 'attr-map? args))))

#?(:clj
   (defmacro defelem
  "Defines a function that will return a element vector. If the first argument
  passed to the resulting function is a map, it merges it with the attribute
  map of the returned element value."
  [name & fdecl]
  (let [fn-name# (gensym (str name))
        fdecl (postwalk-replace {name fn-name#} fdecl)]
    `(do (defn ~fn-name# ~@fdecl)
         (def ~name (sablono.core/wrap-attrs ~fn-name#))))))

#?(:cljs
   (defn render
  "Render `element` as HTML string."
  [element]
  (if element
    (js/React.renderToString element))))

#?(:cljs
   (defn render-static
  "Render `element` as HTML string, without React internal attributes."
  [element]
  (if element
    (js/React.renderToStaticMarkup element))))

(defn include-css
  "Include a list of external stylesheet files."
  [& styles]
  (for [style styles]
    [:link {:type "text/css", :href (as-str style), :rel "stylesheet"}]))

#?(:cljs
   (defn include-js
  "Include the JavaScript library at `src`."
  [src]
  (dom/appendChild
   (.-body (dom/getDocument))
   (dom/createDom "script" #js {:src src}))))

#?(:cljs
   (defn include-react
  "Include Facebook's React JavaScript library."
  [] (include-js "http://fb.me/react-0.12.2.js")))

(defelem link-to
  "Wraps some content in a HTML hyperlink with the supplied URL."
  [url & content]
  [:a {:href (as-str url)} content])

(defelem mail-to
  "Wraps some content in a HTML hyperlink with the supplied e-mail
  address. If no content provided use the e-mail address as content."
  [e-mail & [content]]
  [:a {:href (str "mailto:" e-mail)}
   (or content e-mail)])

(defelem unordered-list
  "Wrap a collection in an unordered list."
  [coll]
  [:ul (for [x coll] [:li x])])

(defelem ordered-list
  "Wrap a collection in an ordered list."
  [coll]
  [:ol (for [x coll] [:li x])])

(defelem image
  "Create an image element."
  ([src] [:img {:src (as-str src)}])
  ([src alt] [:img {:src (as-str src), :alt alt}]))

(def ^:dynamic *group* [])

#?(:clj
   (defmacro with-group
  "Group together a set of related form fields for use with the Ring
  nested-params middleware."
  [group & body]
  `(binding [sablono.core/*group* (conj sablono.core/*group* (as-str ~group))]
     (list ~@body))))

(defn- make-name
  "Create a field name from the supplied argument the current field group."
  [name]
  (reduce #(str %1 "[" %2 "]")
          (conj *group* (as-str name))))

(defn- make-id
  "Create a field id from the supplied argument and current field group."
  [name]
  (reduce #(str %1 "-" %2)
          (conj *group* (as-str name))))

(defn- input-field*
  "Creates a new <input> element."
  [type name value]
  [:input {:type type
           :name (make-name name)
           :id (make-id name)
           :value value}])

#?(:clj
   (defn gen-input-field [input-type]
     (let [fn-name (symbol (str input-type "-field"))
           docstring (str "Creates a " input-type " input field.")]
       `(defelem ~fn-name
          ~docstring
          ([name#] (~fn-name name# nil))
          ([name# value#] (sablono.core/input-field* (str '~input-type) name# value#))))))

#?(:clj
   (defmacro gen-input-fields
  "Generates the input fields."
  []
  (let [fields '[color
                 date
                 datetime
                 datetime-local
                 email
                 file
                 hidden
                 month
                 number
                 password
                 range
                 search
                 tel
                 text
                 time
                 url
                 week]]
    `(do
       ~@(map gen-input-field fields)))))

(gen-input-fields)

(def file-upload file-field)

(defelem check-box
  "Creates a check box."
  ([name] (check-box name nil))
  ([name checked?] (check-box name checked? "true"))
  ([name checked? value]
   [:input {:type "checkbox"
            :name (make-name name)
            :id   (make-id name)
            :value value
            :checked checked?}]))

(defelem radio-button
  "Creates a radio button."
  ([group] (radio-button group nil))
  ([group checked?] (radio-button group checked? "true"))
  ([group checked? value]
   [:input {:type "radio"
            :name (make-name group)
            :id   (make-id (str (as-str group) "-" (as-str value)))
            :value value
            :checked checked?}]))

(defelem select-options
  "Creates a seq of option tags from a collection."
  ([coll] (select-options coll nil))
  ([coll selected]
   (for [x coll]
     (if (sequential? x)
       (let [[text val disabled?] x
             disabled? (boolean disabled?)]
         (if (sequential? val)
           [:optgroup {:label text} (select-options val selected)]
           [:option {:value val :selected (= val selected) :disabled disabled?} text]))
       [:option {:selected (= x selected)} x]))))

(defelem drop-down
  "Creates a drop-down box using the <select> tag."
  ([name options] (drop-down name options nil))
  ([name options selected]
   [:select {:name (make-name name), :id (make-id name)}
    (select-options options selected)]))

(defelem text-area
  "Creates a text area element."
  ([name] (text-area name nil))
  ([name value]
   [:textarea
    {:name (make-name name)
     :id (make-id name)
     :value value}]))

(defelem label
  "Creates a label for an input field with the supplied name."
  [name text]
  [:label {:htmlFor (make-id name)} text])

(defelem submit-button
  "Creates a submit button."
  [text]
  [:input {:type "submit" :value text}])

(defelem reset-button
  "Creates a form reset button."
  [text]
  [:input {:type "reset" :value text}])

(defelem form-to
  "Create a form that points to a particular method and route.
  e.g. (form-to [:put \"/post\"]
         ...)"
  [[method action] & body]
  (let [method-str (upper-case (name method))
        action-uri (to-uri action)]
    (-> (if (contains? #{:get :post} method)
          [:form {:method method-str, :action action-uri}]
          [:form {:method "POST", :action action-uri}
           (hidden-field "_method" method-str)])
        (concat body)
        (vec))))
