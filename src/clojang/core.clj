(ns clojang.core
  (:require [clojure.core.async :as async :refer [<! >! <!! chan go close!]]
            [clojang.types :as types]
            [trptcolin.versioneer.core :as version])
  (:import [com.ericsson.otp.erlang
            OtpErlangDecodeException
            OtpInputStream
            OtpOutputStream
            OtpException])
  (:gen-class))

(defn get-version []
  (version/get-version "" "clojang"))

(defn log [& params]
  (.println System/err (apply str params)))

(defn port-connection []
  (let [in (chan) out (chan)]
    (go ;; term receiver coroutine
      (try 
        (while true 
          (let [len-buf (byte-array 4)]
            (.read System/in len-buf)
            (let [term-len (.read4BE (new OtpInputStream len-buf))
                  term-buf (byte-array term-len)]
              (.read System/in term-buf)
              (let [b (types/decode (.read_any (new OtpInputStream term-buf))) ]
                (>! in b)))))
        (catch Exception e (do 
          (log "receive error : " (type e) " " (.getMessage e)) 
          (close! out) (close! in)))))
    (go ;; term sender coroutine
      (loop []
        (when-let [term (<! out)]
          (try
            (let [out-term (new OtpOutputStream (types/encode term))]
              (doto (new OtpOutputStream) (.write4BE (+ 1 (.size out-term))) (.write 131) (.writeTo System/out))
              (.writeTo out-term System/out)
              (.flush System/out))
            (catch Exception e (log "send error : " (type e) " " (.getMessage e))))
          (recur))))
    [in out] ))

(defn run-server 
  ([handle] (run-server (fn [state] state) handle))
  ([init handle]
    (let [[in out] (port-connection)]
      (<!! (go
        (loop [state (init (<! in))]
          (if-let [req (<! in)]
            (let [res (try (handle req state) (catch Exception e [:stop [:error e]]))]
              (case (res 0)
                :reply (do (>! out (res 1)) (recur (res 2)))
                :noreply (recur (res 1))
                :stop (res 1)))
            :normal)))))))
