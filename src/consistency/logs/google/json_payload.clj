(ns consistency.logs.google.json-payload
  (:require [jsonista.core :as json])
  (:import (java.util.logging LogManager ConsoleHandler SimpleFormatter LogRecord Level)
           (java.io StringWriter PrintWriter)))

(def line-separator (System/getProperty "line.separator"))

(def levels->JUL (merge {:trace Level/FINEST
                         :debug Level/FINE
                         :info Level/INFO
                         :warn Level/WARNING
                         :error Level/SEVERE
                         :fatal Level/SEVERE}
                        {:off Level/OFF
                         :severe Level/SEVERE
                         :warning Level/WARNING
                         :info Level/INFO
                         :config Level/CONFIG
                         :fine Level/FINE
                         :finer Level/FINER
                         :finest Level/FINEST
                         :all Level/ALL}))

(def JUL-levels->int (reduce-kv (fn [coll k ^Level v]
                                  (assoc coll k (.intValue v)))
                                {} levels->JUL))

(def root-logger (.getLogger (LogManager/getLogManager) ""))

(defn record->json-payload [^LogRecord record]
  (let [thrown ^Throwable (.getThrown record)
        level (.getLevel record)
        ex-info? (= clojure.lang.ExceptionInfo (class thrown))]
    (cond->
      {"severity" (.getName level)
       :logger-name (.getLoggerName record)
       "message" (if thrown
                   (let [w (StringWriter.)]
                     (.printStackTrace thrown (PrintWriter. w))
                     (.toString w))
                   (.getMessage record))}
      ex-info? (assoc :ex-info (ex-data thrown))
      (= Level/SEVERE level) (assoc "@type" "type.googleapis.com/google.devtools.clouderrorreporting.v1beta1.ReportedErrorEvent")
      thrown (assoc :logger-message (.getMessage record)))))

(def formatter
  (proxy [SimpleFormatter] []
    (^String format [^LogRecord record]
      (str
        (json/write-value-as-string (record->json-payload record))
        line-separator))))

(def console-handler
  (doto (ConsoleHandler.)
    (.setLevel Level/ALL)
    (.setFormatter formatter)))

(def formatter-pretty
  (proxy [SimpleFormatter] []
    (^String format [^LogRecord record]
      (str
        (json/write-value-as-string (record->json-payload record) (json/object-mapper {:pretty true}))
        line-separator))))

(def console-handler-pretty
  (doto (ConsoleHandler.)
    (.setLevel Level/ALL)
    (.setFormatter formatter-pretty)))