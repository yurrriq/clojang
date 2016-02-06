(ns clojang.util
  (:require [clojure.core.typed :as t :refer [ann defalias]]
            [clojure.string :as string]
            [dire.core :refer [with-handler!]])
  (:import [clojure.lang Named Reflector Symbol Var]
           [com.ericsson.otp.erlang]))

;; XXX support the following keys:
;; [-d|-debug] [DbgExtra...] [-port No] [-daemon] [-relaxed_command_check]
(defn start-epmd
  "Start the Erlang Port Mapper Daemon external (OS) process needed by
  JInterface for creating nodes and communicating with other nodes."
  []
  'TBD)

(defn convert-class-name
  "A helper function for use when creating Erlang class wrappers."
  [name-symbol]
  (case (str name-symbol)
    ;; Types
    "external-fun" "ExternalFun"
    "list-sublist" "List$SubList"
    "object-hash" "Object$Hash"
    "uint" "UInt"
    "ushort" "UShort"
    ;; OTP
    "local-node" "LocalNode"
    ;; Everything else
    (string/capitalize name-symbol)))

(defn make-jinterface-name
  "A helper function for use when defining constructor macros."
  [prefix name-symbol]
  (->> name-symbol
       (str)
       (convert-class-name)
       (str "com.ericsson.otp.erlang." prefix)
       (symbol)))

;; XXX: This one might be worth fixing/finishing.
(ann ^:no-check dynamic-init
     [[String -> Symbol] String Object * -> (t/Nilable Object)])
(defn dynamic-init
  "Dynamically instantiates classes based upon a transformation function and
  a symbol used by the transformation function to create a class name that is
  ultimately resolvable."
  [name-gen-fn name-part & args]
  (Reflector/invokeConstructor
    (resolve (name-gen-fn name-part))
    (t/into-array> Object args)))

;; XXX: It's not worth the extra effort.
(ann ^:no-check get-hostname [-> String])
(defn get-hostname
  "Get the hostname for the machine that this JVM is running on.

  Uses the ``java.net.InetAddress`` methods ``getLocalHost`` and
  ``getHostName``."
  []
  (.. java.net.InetAddress (getLocalHost) (getHostName)))

(defn add-err-handler
  "A wrapper for generating a specific dire error handler."
  ([handled-fn excep]
    (add-err-handler
      handled-fn
      excep
      "[ERROR] There was a problem!"))
  ([handled-fn excep msg]
    (with-handler! handled-fn
      excep
      (fn [e & args]
        (println msg)
        (println (str {:args args :errors e}))))))

(ann ->str-arg [(t/U Named String) -> String])
(def ->str-arg "Equivalent to ``#'clojure.core/name``" name)

(ann ->str-args [(t/Seqable (t/U Named String)) -> (t/Vec String)])
(defn ->str-args [args]
  (reduce (t/fn [acc :- (t/Vec String), x :- (t/U Named String)]
            (conj acc (->str-arg x)))
          [] args))
