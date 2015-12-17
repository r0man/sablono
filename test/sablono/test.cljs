(ns sablono.test
  (:require [doo.runner :refer-macros [doo-tests]]
            [sablono.benchmark]
            [sablono.core-test]
            [sablono.interpreter-test]
            [sablono.normalize-test]
            [sablono.util-test]))

(doo-tests 'sablono.core-test
           'sablono.interpreter-test
           'sablono.normalize-test
           'sablono.util-test
           'sablono.benchmark)
