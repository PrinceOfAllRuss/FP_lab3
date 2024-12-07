(ns point
  (:require [clojure.string :refer [split]]))

(defrecord Point [x y])

(defn to-string [^Point point]
  (str "(" (:x point) ";" (:y point) ")"))

(defn parse-from-line [line]
  (let [split (split line #";")]
    (if (= (count split) 2)
      (->Point (parse-double (first split)) (parse-double (second split)))
      (throw (Exception. "Invalid string passed")))))
