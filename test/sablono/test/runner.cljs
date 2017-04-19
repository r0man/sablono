(ns sablono.test.runner
  (:require [clojure.test.check]
            [clojure.test.check.generators]
            [doo.runner :refer-macros [doo-tests]]
            [sablono.core-test]
            [sablono.input-test]
            [sablono.interpreter-test]
            [sablono.normalize-test]
            [sablono.server-test]
            [sablono.specs-test]
            [sablono.test]
            [sablono.util-test]))

(doo-tests 'sablono.core-test
           'sablono.interpreter-test
           'sablono.normalize-test
           'sablono.server-test
           'sablono.specs-test
           'sablono.util-test)
