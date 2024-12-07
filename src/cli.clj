(ns cli
  (:refer-clojure :exclude [read])
  (:require [clojure.string :refer [split trim join]]
            [methods :refer [describe interpolate]]
            [point :refer [to-string]]
            [reader :refer [read]]))

(defrecord Command [name usage docstring parse default required?])

(def options {"--help"    (->Command "help" "--help" "Display help message"
                                     nil nil false)
              "-enclosed" (->Command "enclosed" "-enclosed WINDOW" "Creates a window of the specified size and passes it through the data set. When WINDOW = 0, the general mode is used."
                                     #(parse-double %) "0" false)
              "-step"         (->Command "step" "-step X" "The value of the step x for finding the function in (f(x0 + x) = ?)"
                                         #(parse-double %) "1" false)
              "-a" (->Command "a" "-a algorithm1, algorithm2" "Interpolation algorithm(s) (\"lagrange\", \"linear\")"
                              #(map trim (split % #",")) ["lagrange"] false)})

(defn- display-usage []
  (println "Usage: cli <args>")
  (println "Available arguments: ")
  (doseq [[_ v] options]
    (println (str (:name v) ": "
                  (:usage v) ", "
                  (:docstring v) " "
                  (if (:required? v) "-- Required" "")
                  (if (:default v) (str "-- Default: " (:default v)) "")))))

(defn parse-options [opts]
  (let [opts (into {} (into [] (map #(into [] %) (partition 2 opts))))
        required (->> options
                      (filter #(:required? (second %)))
                      (mapv first))
        defaults (->> options
                      (filter #(not (nil? (:default (second %)))))
                      (map (fn [[k v]] [k (:default v)]))
                      (into {}))
        opts (conj defaults opts)
        required-opts-size (->> required
                                (map #(get opts %))
                                (filter #(not (nil? %)))
                                (count))]
    (if (< (count required) required-opts-size)
      nil
      (try
        (into {} (map (fn [[k v]]
                        (let [option (get options k)]
                          (when (nil? option)
                            (throw (Exception. "Invalid arguments")))
                          [k ((:parse option) v)])) opts))
        (catch Exception _ nil)))))

(defn -main [& opts]
  (let [opts (parse-options opts)]
    (if (or (nil? opts) (contains? opts "--help"))
      (display-usage)
      (println ((fn [init]
                  (let [point (read)]
                    (if (nil? point)
                      "EOF"
                      (let [list (conj init point)]
                        (println (str "-> " (join " " (reverse (map to-string list)))))
                        (doseq [algo (get opts "-a")]
                          (println (describe algo (interpolate algo (get opts "-enclosed") list (get opts "-step")))))
                        (recur list))))) '())))))