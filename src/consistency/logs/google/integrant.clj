(ns consistency.logs.google.integrant
  (:require [integrant.core :as ig]
            [clojure.spec.alpha :as s]
            [consistency.logs.google.json-payload :as google-json-payload])
  (:import (java.util.logging ConsoleHandler Level LogManager$RootLogger)))

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

(defmethod ig/init-key :logs/google-json-payload [_ {:keys [level pretty?]}]
  (let [JUL-level (get google-json-payload/levels->JUL level)
        console-handler (if pretty?
                          google-json-payload/console-handler-pretty
                          google-json-payload/console-handler)]
    (remove-root-logger-handlers)
    (.setLevel ^LogManager$RootLogger google-json-payload/root-logger JUL-level)
    (.addHandler ^LogManager$RootLogger google-json-payload/root-logger console-handler)))

(defmethod ig/halt-key! :logs/google-json-payload [_ initialized]
  (remove-root-logger-handlers)
  (.addHandler ^LogManager$RootLogger google-json-payload/root-logger (doto (ConsoleHandler.)
                                                                        (.setLevel Level/ALL))))