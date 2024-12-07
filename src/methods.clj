(ns methods
  (:refer-clojure :exclude [replace])
  (:require [clojure.string :refer [join replace]]))

(defmulti interpolate-x (fn [type _ _] type))

(defn- calc-polynomial-for-point [point points target]
  (reduce (fn [acc cur]
            (if (= point cur)
              acc
              (* acc (/ (- target (:x cur)) (- (:x point) (:x cur)))))) 1 points))

(defmethod interpolate-x "lagrange" [_ points target]
  (reduce (fn [acc point]
            (let [poly (calc-polynomial-for-point point points target)]
              (+ acc (* (:y point) poly)))) 0 points))

(defmethod interpolate-x "linear" [_ points target]
  ((fn [points cur left]
     (if (nil? cur)
       nil
       (if (> target (:x cur))
         (recur (drop 1 points) (first points) cur)
         (if (< target (:x cur))
           (if (nil? left)
             nil
             (+ (:y left) (* (/ (- target (:x left)) (- (:x cur) (:x left))) (- (:y cur) (:y left)))))
           (if (= target (:x cur))
             (:y cur)
             nil))))) (drop 1 points) (first points) nil))

(defn check-enclosed [window-size points]
  (or (zero? window-size) (zero? (count points)) (<= (- (:x (first points)) (:x (last points))) window-size)))

(defn- round-to [num decimals]
  (/ (Math/round (* num (Math/pow 10 decimals))) (Math/pow 10 decimals)))

(defn interpolate [algo window-size points step]
  (let [points ((fn [points]
                  (if (check-enclosed window-size points)
                    points
                    (recur (drop-last 1 points)))) points)]
    (if (< (count points) 2)
      nil
      (map (fn [x] [(round-to x 7) (interpolate-x algo (reverse points) (round-to x 7))])
           (take-while
            (partial > (:x (first points)))
            (iterate (partial + step) (+ step (:x (second points)))))))))

(defmulti describe (fn [t _] t))

(defn- process-result [result]
  (if (or (nil? result) (zero? (count result)))
    "Not enough data."
    (join "; " (map #(str "f(" (format "%.3f" (first %)) ")=" (format "%.3f" (second %))) result))))

(defmethod describe "lagrange" [_ result]
  (str "Interpolated by Lagrange: " (replace (process-result result) #"," ".")))

(defmethod describe "linear" [_ result]
  (str "Interpolated by Linear: " (replace (process-result result) #"," ".")))