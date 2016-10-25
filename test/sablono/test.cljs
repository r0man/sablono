(ns sablono.test
  (:require [doo.runner :refer-macros [doo-tests]]
            [sablono.core-test]
            [sablono.interpreter-test]
            [sablono.normalize-test]
            [sablono.server-test]
            [sablono.util-test]))

(doo-tests 'sablono.core-test
           'sablono.interpreter-test
           'sablono.normalize-test
           'sablono.server-test
           'sablono.util-test)
