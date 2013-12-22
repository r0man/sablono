(ns sablono.util
  #+cljs (:import goog.Uri))

(def ^:dynamic *base-url* nil)

(defprotocol ToString
  (to-str [x] "Convert a value into a string."))

(defprotocol ToURI
  (to-uri [x] "Convert a value into a URI."))

(defn as-str
  "Converts its arguments into a string using to-str."
  [& xs]
  (apply str (map to-str xs)))

#+cljs
(extend-protocol ToString
  cljs.core.Keyword
  (to-str [x]
    (name x))
  goog.Uri
  (to-str [x]
    (if (or (. x (hasDomain))
            (nil? (. x (getPath)))
            (not (re-matches #"^/.*" (. x (getPath)))))
      (str x)
      (let [base (str *base-url*)]
        (if (re-matches #".*/$" base)
          (str (subs base 0 (dec (count base))) x)
          (str base x)))))
  nil
  (to-str [_]
    "")
  number
  (to-str [x]
    (str x))
  default
  (to-str [x]
    (str x)))

#+cljs
(extend-protocol ToURI
  Uri
  (to-uri [x] x)
  default
  (to-uri [x] (Uri. (str x))))
