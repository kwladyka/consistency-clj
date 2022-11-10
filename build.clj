(ns build
  (:refer-clojure :exclude [test])
  (:require [org.corfield.build :as bb]))

(def lib 'net.clojars.kwladyka/consistency)

(defn clean [opts]
  (bb/clean opts))

(defn test [opts]
  (bb/run-tests opts))

(defn jar [opts]
  (-> opts
    (assoc :lib lib
           :scm {:url "https://github.com/kwladyka/consistency-clj"})
    (bb/jar)))

(defn deploy [opts]
  (-> opts
      (assoc :lib lib :sign-releases? true)
      (bb/deploy)))