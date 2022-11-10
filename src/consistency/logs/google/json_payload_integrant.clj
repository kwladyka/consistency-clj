(ns consistency.logs.google.json-payload-integrant
  (:require [integrant.core :as ig]
            [clojure.spec.alpha :as s]
            [consistency.logs.google.json-payload :as google-json-payload])
  (:import (java.util.logging ConsoleHandler Level Filter LogRecord LogManager$RootLogger)))

(s/def ::level (s/nilable (set (keys google-json-payload/levels->JUL))))
(s/def ::pretty? boolean?)

(defmethod ig/pre-init-spec :logs/google-json-payload [_]
  (s/keys :req-un [::level]
          :opt-un [::pretty?]))

(defmethod ig/prep-key :logs/google-json-payload [_ config]
  (update config :level #(or % :info)))

(defn remove-root-logger-handlers []
  (doseq [handler (.getHandlers ^LogManager$RootLogger google-json-payload/root-logger)]
    (.removeHandler ^LogManager$RootLogger google-json-payload/root-logger handler)))

(defmethod ig/init-key :logs/google-json-payload [_ {:keys [level pretty? is-loggable?]}]
  (let [JUL-level (get google-json-payload/levels->JUL level)
        console-handler (if pretty?
                          google-json-payload/console-handler-pretty
                          google-json-payload/console-handler)
        logger-filter (proxy [Filter] []
                        (^Boolean isLoggable [^LogRecord record]
                          (is-loggable? {:logger-name (.getLoggerName record)
                                         :level (.getLevel record)})))]
    (remove-root-logger-handlers)
    (.setLevel ^LogManager$RootLogger google-json-payload/root-logger JUL-level)
    (if filter
      (.setFilter ^ConsoleHandler console-handler ^Filter logger-filter)
      (.setFilter ^ConsoleHandler console-handler nil))
    (.addHandler ^LogManager$RootLogger google-json-payload/root-logger console-handler)))

(defmethod ig/halt-key! :logs/google-json-payload [_ initialized]
  (remove-root-logger-handlers)
  (.addHandler ^LogManager$RootLogger google-json-payload/root-logger (doto (ConsoleHandler.)
                                                                        (.setLevel Level/ALL))))