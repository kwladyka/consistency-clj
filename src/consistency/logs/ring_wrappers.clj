(ns consistency.logs.ring-wrappers
  (:require [clojure.tools.logging :as l]))

(defn wrap-request [handler]
  (fn [{:keys [body] :as request}]
    (if (and body
             (#{"application/json"} (get-in request [:headers "content-type"])))
      (do (l/debug {:request (pr-str (update request :body slurp))})
          (.reset ^java.io.ByteArrayInputStream body))
      (l/debug {:request (pr-str request)}))
    (handler request)))

(defn wrap-response [handler]
  (fn [request]
    (let [{:keys [status] :as response} (handler request)]
      (if (#{200 404} status)
        (l/debug {:response (pr-str response)})
        (l/info {:response (pr-str response)}))
      response)))

(defn wrap-exceptions [handler]
  (fn [{:keys [body] :as request}]
    (try
      (handler request)
      (catch Throwable ex
        (if body
          (l/error ex {:request (update request :body slurp)})
          (l/error ex {:request request}))
        {:status 500
         :body "500 Internal Server Error"}))))