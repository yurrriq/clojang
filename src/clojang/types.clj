(ns clojang.types
  (:import [com.ericsson.otp.erlang
            OtpErlangAtom
            OtpErlangBinary
            OtpErlangBitstr
            OtpErlangDouble
            OtpErlangFloat
            OtpErlangByte
            OtpErlangChar
            OtpErlangShort
            OtpErlangUShort
            OtpErlangLong
            OtpErlangInt
            OtpErlangUInt
            OtpErlangList
            OtpErlangPid
            OtpErlangPort
            OtpErlangRef
            OtpErlangFun
            OtpErlangExternalFun
            OtpErlangTuple
            OtpErlangMap
            OtpErlangObject
            OtpErlangString
            OtpErlangBoolean])
  (:gen-class))

(declare encode decode)

;;; atoms

(defmulti erl-atom class)
(defmethod erl-atom String [s] :a-string)
(defmethod erl-atom clojure.lang.Keyword [s] :a-keyword)

(defn erl-atom [str]
  (OtpErlangAtom. str))

(defn erl-atom [kwd]
  (OtpErlangAtom. (name kwd)))

(defn erl-atom? [x]
  (= (type x) OtpErlangAtom))

(defn ->atom [obj]
  (let [k (keyword (.atomValue obj))]
    (cond
      (#{:true :false} k) (= k :true)
      (#{:nil :null :none :undefined} k) nil
      :else k)))

;;; tuples

(defn erl-tuple [vec]
  (OtpErlangTuple. (into-array OtpErlangObject (map encode vec))))

(defn erl-tuple? [x]
  (= (type x) OtpErlangTuple))

(defn ->tuple [obj]
  (vec (map decode (.elements obj))))

;;; strings

(defn erl-str [str]
  (OtpErlangString. str))

(defn erl-str? [x]
  (= (type x) OtpErlangString))

(defn ->str [erl]
  (.stringValue erl))

;;; lists

(defn erl-list [lst]
  (OtpErlangList. (into-array OtpErlangObject (map encode lst))))

(defn erl-list? [x]
  (= (type x) OtpErlangList))

(defn ->list [obj]
  (seq (map decode (.elements obj))))

;;; maps

(defn erl-map [a-map]
  (OtpErlangMap. (into-array OtpErlangObject (map encode (keys a-map)))
                 (into-array OtpErlangObject (map encode (vals a-map)))))

(defn erl-map? [x]
  (= (type x) OtpErlangMap))

(defn ->map [obj]
  (zipmap (map decode (.keys obj))
          (map decode (.values obj))))

;;; longs

(defn erl-long [lng]
  (OtpErlangLong. (long lng)))

(defn erl-long? [x]
  (= (type x) OtpErlangLong))

(defn ->long [obj]
  (.longValue obj))

;;; doubles

(defn erl-double [dbl]
  (OtpErlangDouble. (double dbl)))

(defn erl-double? [x]
  (= (type x) OtpErlangDouble))

(defn ->double [obj]
  (.doubleValue obj))

;;; bytes

(defn erl-bytes [bs]
  (OtpErlangBinary. (bytes bs)))

;;; booleans

(defn erl-bool [bool]
  (OtpErlangBoolean. bool))

(defn erl-bool? [x]
  (= (type x) OtpErlangBoolean))

(defn ->bool [obj]
  (.booleanValue obj))

;;; binaries

(defn erl-bin [bin]
  (OtpErlangBinary. (bytes (.getBytes bin "UTF-8"))))

(defn erl-bin? [x]
  (= (type x) OtpErlangBinary))

(defn ->bin [obj]
  (.binaryValue obj))

;;; Convenience functions for Clojure types

(defn bool? [x]
  (= java.lang.Boolean (type x)))

(defn bytes? [x]
  (= (class (byte-array [])) (type x)))

;;; Coder functions

(defn decode [obj]
  (cond
    (erl-atom? obj) (->atom obj)
    (erl-list? obj) (->list obj)
    (erl-tuple? obj) (->tuple obj)
    (erl-str? obj) (->str obj)
    (erl-bin? obj) (->bin obj)
    (erl-map? obj) (->map obj)
    (erl-long? obj) (->long obj)
    (erl-double? obj) (->double obj)
    (erl-bool? obj) (->bool obj)
    :else   :decoding-error))

(defn encode [obj]
  (cond
    (nil? obj) (erl-atom :nil)
    (seq? obj) (erl-list obj)
    (set? obj) (erl-list obj)
    (vector? obj) (erl-tuple obj)
    (string? obj) (erl-str obj)
    (keyword? obj) (erl-atom obj)
    (integer? obj) (erl-long obj)
    (float? obj) (erl-double obj)
    (map? obj) (erl-map obj)
    (bytes? obj) (erl-bytes obj)
    (bool? obj) (erl-bool obj)
    :else   (encode (str obj))))
