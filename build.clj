(ns build
  (:refer-clojure :exclude [test])
  (:require [org.corfield.build :as bb]))

(def lib 'data-collector/start)
(def main 'data-collector.start)
(def jar-path "target/app.jar")

(defn clean [opts]
  (bb/clean opts))

(defn test [opts]
  (bb/run-tests opts))

(defn uber [opts]
  (-> (assoc opts
             :lib lib
             :main main
             :uber-file jar-path)
      (bb/uber)))

(defn ci [opts]
  (-> opts
      (test)
      (clean)
      (uber)))
