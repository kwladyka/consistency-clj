(ns consistency.logs.google.json-payload-integrant-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [consistency.logs.google.json-payload-integrant :as google-integrant]
            [consistency.logs.google.json-payload :as google-json-payload]
            [clojure.tools.logging :as l])
  (:import (java.util.logging ConsoleHandler Level Filter LogRecord LogManager$RootLogger)))

(defonce state nil)

(defn stop-json-payload []
  (when state
    (alter-var-root #'state ig/halt!)))

(defn restart-json-payload
  "level is :debug, but filter is :warn for this ns"
  []
  (let [config {:logs/google-json-payload {:level :debug
                                           :is-loggable? (fn [{:keys [^String logger-name ^Level level]}]
                                                           (let [level-int (.intValue level)]
                                                             (not
                                                               (or (and (.startsWith logger-name "foo.bar.baz")
                                                                        (< level-int (google-json-payload/JUL-levels->int :debug)))
                                                                   (and (.startsWith logger-name "consistency.logs.google.json-payload-integrant-test")
                                                                        (< level-int (google-json-payload/JUL-levels->int :warn)))))))
                                           :pretty? true}}]
    (stop-json-payload)
    (alter-var-root #'state
                    (fn [_]
                      (ig/init (ig/prep config))))))

;; manual tests

(comment
  (restart-json-payload)
  (stop-json-payload)

  (let [logger-filter (proxy [Filter] []
                        (^Boolean isLoggable [^LogRecord record]
                          (let [level (.getLevel record)
                                logger-name (.getLoggerName record)]
                            (println "level" level "logger-name" logger-name)
                            ;(spit "foo.edn" {:level level :logger-name logger-name})
                            true)))]
    (.setFilter ^ConsoleHandler google-json-payload/console-handler-pretty ^Filter logger-filter)
    (l/info "foo"))

  (doseq [loggers (enumeration-seq (.getLoggerNames (java.util.logging.LogManager/getLogManager)))
          :let [logger (.getLogger (java.util.logging.LogManager/getLogManager) loggers)
                handlers (.getHandlers logger)]]
    (.setLevel logger java.util.logging.Level/ALL)
    (doseq [handler handlers]
      (.setLevel handler java.util.logging.Level/ALL)))

  (.fine
    (.getLogger (java.util.logging.LogManager/getLogManager) "")
    "fine")

  (clojure.core/Throwable->map)

  (l/spy :debug "foo")
  (l/spy :info "foo")
  (l/trace "foo")
  (l/debug "foo")
  (l/info "foo")
  (l/warn "foo")
  (l/error "foo")

  (try
    (try
      (/ 1 0)
      (catch Exception e
        (throw
          (ex-info "foo ex" {:cause :foo} e))))
    (catch Exception e
      (l/error e "exception test with ex-info and cause")))

  (try
    (throw
      (ex-info "foo ex" {:cause :foo}))
    (catch Exception e
      (l/error e "exception test with ex-info")))

  (try
    (/ 1 0)
    (catch Exception e
      (l/error e "exception test"))))