(ns sablono.checksum
  #?(:cljs (:import [goog.string StringBuffer])))

(def MOD 65521)

(defn char-code-at
  "Return the character as an integer at the specified index of `sb`."
  [^StringBuffer sb index]
  #?(:clj (int (.charAt sb index))
     :cljs (int (.charCodeAt sb index))))

(defn adler32
  "Return the Adler-32 checksum of `sb`."
  [^StringBuffer sb]
  (let [l (#?(:clj .length :cljs .getLength) sb)
        d #?(:clj sb :cljs (.toString sb))
        m (bit-and l -4)]
    (loop [a (int 1)
           b (int 0)
           i 0
           n (min (+ i 4096) m)]
      (cond
        (< i n)
        (let [c0 (char-code-at d i)
              c1 (char-code-at d (+ i 1))
              c2 (char-code-at d (+ i 2))
              c3 (char-code-at d (+ i 3))
              b  (+ b a c0
                    a c0 c1
                    a c0 c1 c2
                    a c0 c1 c2 c3)
              a  (+ a c0 c1 c2 c3)]
          (recur (rem a MOD) (rem b MOD) (+ i 4) n))

        (< i m)
        (recur a b i (min (+ i 4096) m))

        (< i l)
        (let [c0 (char-code-at d i)]
          (recur (+ a c0) (+ b a c0) (+ i 1) n))

        :else
        (let [a (rem a MOD)
              b (rem b MOD)]
          (bit-or (int a) (unchecked-int (bit-shift-left b 16))))))))
