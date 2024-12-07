(ns reader
  (:refer-clojure :exclude [read])
  (:require [point :refer [parse-from-line]]))

(defn read []
  (let [line (try (read-line)
                  (catch Exception _ nil))]
    (if (nil? line)
      nil
      (parse-from-line line))))
