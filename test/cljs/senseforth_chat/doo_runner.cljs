(ns senseforth-chat.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [senseforth-chat.core-test]))

(doo-tests 'senseforth-chat.core-test)

