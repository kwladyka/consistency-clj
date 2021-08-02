(ns consistency.logs.google.json-payload-integrant-test
  (:require [clojure.test :refer :all]
            [integrant.core :as ig]
            [consistency.logs.google.json-payload-integrant :as google-integrant]
            [clojure.tools.logging :as l])
  (:import (java.util.logging Level LogManager)))

(defonce state nil)

(defn stop-json-payload []
  (when state
    (alter-var-root #'state ig/halt!)))

(defn restart-json-payload []
  (let [config {:logs/google-json-payload {:level :debug
                                           :pretty? true}}]
    (stop-json-payload)
    (alter-var-root #'state
                    (fn [_]
                      (ig/init (ig/prep config))))))

;; manual tests

(comment
  (restart-json-payload)
  (stop-json-payload)

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