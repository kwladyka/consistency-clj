(ns consistency.logs.ring-wrappers-test
  (:require [clojure.test :refer :all]
            [peridot.core :as peridot]
            [clojure.tools.logging :as log]
            [clojure.tools.logging.test :as log-test]
            [consistency.logs.ring-wrappers :as logs-ring-wrappers]))

(defn log-entry-with-thread
  [logger-ns level throwable message]
  (assoc (log-test/->LogEntry logger-ns level throwable message)
    ::thread (Thread/currentThread)))

(defmacro with-log
  [& body]
  `(let [stateful-log# (log-test/atomic-log log-entry-with-thread)
         logger-factory# (log-test/logger-factory stateful-log# (constantly true))]
     (binding [log-test/*stateful-log* stateful-log#
               log/*logger-factory* logger-factory#]
       ~@body)))

(deftest wrappers-test
  (testing "request"
    (let [session (peridot/session
                    (logs-ring-wrappers/wrap-request (constantly {})))]
      (with-log
        (-> (peridot/content-type session "application/json")
            (peridot/request "/"
                             :request-method :post
                             :body "{\"foo\":\"bar\"}"))
        (is
          (log-test/logged?
            "consistency.logs.ring-wrappers" :debug nil
            "{:request {:protocol \"HTTP/1.1\", :remote-addr \"127.0.0.1\", :headers {\"host\" \"localhost\", \"content-type\" \"application/json\"}, :server-port 80, :content-type \"application/json\", :uri \"/\", :server-name \"localhost\", :body \"{\\\"foo\\\":\\\"bar\\\"}\", :scheme :http, :request-method :post}}")
          "JSON request"))

      (with-log
        (-> (peridot/content-type session "application/json")
            (peridot/request "/" :request-method :post))
        (is
          (log-test/logged?
            "consistency.logs.ring-wrappers" :debug nil
            "{:request {:protocol \"HTTP/1.1\", :remote-addr \"127.0.0.1\", :headers {\"host\" \"localhost\", \"content-type\" \"application/json\"}, :server-port 80, :content-type \"application/json\", :uri \"/\", :server-name \"localhost\", :body nil, :scheme :http, :request-method :post}}")
          "JSON request without body"))

      (with-log
        (peridot/request session "/")
        (is
          (log-test/logged?
            "consistency.logs.ring-wrappers" :debug nil
            "{:request {:protocol \"HTTP/1.1\", :remote-addr \"127.0.0.1\", :headers {\"host\" \"localhost\"}, :server-port 80, :uri \"/\", :server-name \"localhost\", :body nil, :scheme :http, :request-method :get}}")
          "without content-type header"))))

  (testing "response"
    (let [request (fn [response]
                    (-> (logs-ring-wrappers/wrap-response (constantly response))
                        (peridot/session)
                        (peridot/request "/")))]
      (with-log
        (request {:status 200})
        (is
          (log-test/logged?
            "consistency.logs.ring-wrappers" :debug nil
            "{:response {:status 200}}")
          "debug HTTP 200"))

      (with-log
        (request {:status 404})
        (is
          (log-test/logged?
            "consistency.logs.ring-wrappers" :debug nil
            "{:response {:status 404}}")
          "debug HTTP 404"))

      (with-log
        (request {:status 500})
        (is
          (log-test/logged?
            "consistency.logs.ring-wrappers" :info nil
            "{:response {:status 500}}")
          "info HTTP 500"))))

  (testing "exceptions"
    (let [e (Exception.)
          session (peridot/session
                    (logs-ring-wrappers/wrap-exceptions (fn [& _] (throw e))))]
      (with-log
        (peridot/request session "/"
                         :request-method :post
                         :body "foo")
        (is
          (log-test/logged? "consistency.logs.ring-wrappers"
                            :error
                            e
                            "{:request {:protocol HTTP/1.1, :remote-addr 127.0.0.1, :headers {host localhost, content-type application/x-www-form-urlencoded}, :server-port 80, :content-type application/x-www-form-urlencoded, :uri /, :server-name localhost, :body foo, :scheme :http, :request-method :post}}")
          "without body in request"))

      (with-log
        (peridot/request session "/")
        (is
          (log-test/logged? "consistency.logs.ring-wrappers"
                            :error
                            e
                            "{:request {:protocol HTTP/1.1, :remote-addr 127.0.0.1, :headers {host localhost}, :server-port 80, :uri /, :server-name localhost, :body nil, :scheme :http, :request-method :get}}")
          "with body in request")))))

(comment
  (clojure.pprint/pprint (log-test/the-log))

  (with-log
    (log/info "foo")
    (log-test/the-log)))