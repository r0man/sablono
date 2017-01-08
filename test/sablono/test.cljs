(ns sablono.test
  (:require [doo.runner :refer-macros [doo-tests]]
            [sablono.core-test]
            [sablono.interpreter-test]
            [sablono.normalize-test]
            [sablono.server-test]
            [sablono.util-test]))

;; React logs warnings as errors. Needed for lein-doo to not exit
;; PhantomJS. See: https://github.com/bensu/doo/pull/82 and
;; https://github.com/bensu/doo/issues/83
(set! (.-error js/console) (fn [x] (.log js/console x)))

(doo-tests 'sablono.core-test
           'sablono.interpreter-test
           'sablono.normalize-test
           'sablono.server-test
           'sablono.util-test)
