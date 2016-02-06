(ns clojang.jinterface.otp
  (:require [clojure.core.typed :as t :refer [ann ann-protocol]]
            [clojang.util :as util])
  (:import [clojure.lang Named Symbol]))

(ann make-otp-name [(t/U Named String) -> Symbol])
(defn make-otp-name [name-symbol]
  "Given a symbol representing an OTP object name, this function generates
  a JInterface classname as a symbol, resolvable to an imported class."
  (util/make-jinterface-name "Otp" name-symbol))

;;; XXX fix this
(ann ^:no-check init [(t/U Named String) Object * -> (t/Nilable Object)])
(defn init [name-symbol & args]
  "Common function for node instantiation.

  Having a single function which is ultimately responsible for creating
  objects allows us to handle instantiation errors easily, adding one handler
  for ``#'init`` instead of a bunch of handlers, one for each type of node."
  (apply #'util/dynamic-init
         #'make-otp-name name-symbol
         args))
