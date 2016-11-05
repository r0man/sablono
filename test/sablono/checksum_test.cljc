(ns sablono.checksum-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest are]]
            [sablono.checksum :as chk])
  #?(:cljs (:import [goog.string StringBuffer])))

(defn remove-whitespace [s]
  (str/replace s #"(>)\s+(<)" "$1$2"))

(deftest test-adler32
  (are [data res] (= (chk/adler32 (StringBuffer. data)) res)
    "x" 7929977
    "<div data-reactid=\".p55bcrvgg0\"></div>" -47641439
    "<div data-reactid=\".0\" id=\"foo\">Hello World</div>" -1847259110
    (remove-whitespace "<div data-reactid=\".0\">
                             <div data-reactid=\".0.0\">
                               <button data-reactid=\".0.0.0\">Load children</button>
                               <ul data-reactid=\".0.0.1\">
                                 <li data-reactid=\".0.0.1.0\">1</li>
                                 <li data-reactid=\".0.0.1.1\">2</li>
                                 <li data-reactid=\".0.0.1.2\">3</li>
                               </ul>
                             </div>
                           </div>") 821447442
    "Lorem ipsum dolor sit amet, ea nam mutat probatus. Erat volutpat liberavisse eu his,
 id qui eius congue accumsan. Pro erat natum ponderum ut. Vidit assum eu eam. Mei animal
 epicurei facilisi te. In eum euismod principes, id soluta volutpat pri. Nulla harum ex has,
 aliquam verterem recteque has eu. Cu noster utamur quaestio quo, eos eius diceret ei. Ponderum
 atomorum has et. Mel amet dolores philosophia ut, eam id erat noluisse postulant. Sit vide
 regione eu. Ne vis justo liber." -1507348871))
