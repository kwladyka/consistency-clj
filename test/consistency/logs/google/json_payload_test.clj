(ns consistency.logs.google.json-payload-test
  (:require [clojure.test :refer :all]
            [consistency.logs.google.json-payload :as json-payload]
            [jsonista.core :as json]
            clojure.data)
  (:import (java.util.logging Level LogRecord)))

(defn message->LogRecord
  ([message] (message->LogRecord Level/INFO message))
  ([level message]
   (doto (LogRecord. level message)
     (.setLoggerName "ns"))))

(deftest json-payload-test
  (testing "formatter"
    (is
      (= (.format json-payload/formatter (message->LogRecord "foo"))
         "{\"severity\":\"INFO\",\"logger-name\":\"ns\",\"message\":\"foo\"}\n")
      "string log")

    (is
      (re-matches #"(?s)java\.lang\.Exception: bar\n.+"
                  (-> (.format json-payload/formatter
                               (doto (message->LogRecord "foo")
                                 (.setThrown (Exception. "bar"))))
                      (json/read-value)
                      (get "message")))
      "exception")

    (is
      (= (-> (.format json-payload/formatter
                      (doto (message->LogRecord "foo")
                        (.setThrown (ex-info "bar"
                                             {:cause :baz}))))
             (json/read-value)
             (get "ex-info"))
         {"cause" "baz"})
      "ex-info")

    (is
      (re-matches #"(?s)clojure\.lang\.ExceptionInfo: bar \{:cause :baz\}.+Caused by: java\.lang\.Exception: cause exception foo.+"
                  (-> (.format json-payload/formatter
                               (doto (message->LogRecord "foo")
                                 (.setThrown (ex-info "bar"
                                                      {:cause :baz}
                                                      (Exception. "cause exception foo")))))
                      (json/read-value)
                      (get "message")))
      "ex-info with cause")))