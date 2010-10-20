(ns com.failurous.failurous-ring
  (:require [clj-http.client :as http])
  (:require [clojure.contrib.json :as json])
  (:import [java.io PrintWriter StringWriter]))

(defn- stacktrace [e]
  (let [sw (StringWriter.)]
    (.printStackTrace e (PrintWriter. sw))
    (.toString sw)))

(defn- top-of-stacktrace [e]
  (str (aget (.getStackTrace e) 0)))

(defn- make-report [req e]
  (json/json-str
    {:title (.getMessage e)
     :location (top-of-stacktrace e)
     :data [[:summary [[:type (.. e getClass getCanonicalName) {:use_in_checksum true}]
                       [:message (.getMessage e)]
                       [:top_of_stacktrace (top-of-stacktrace e) {:use_in_checksum true}]]]
            [:details [[:stacktrace (stacktrace e)]]]
            [:request (seq req)]]}))

(defn- send-report [req e url api-key]
  (http/post
    (str url "api/projects/" api-key "/fails")
    {:body (make-report req e)}))
  
(defn wrap-failurous [app url api-key]
  (fn [req]
    (try
      (app req)
      (catch Throwable e
        (send-report req e url api-key)
        (throw e)))))
