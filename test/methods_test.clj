(ns methods-test
  (:require [clojure.test :refer [deftest testing is]]
            [methods :refer [interpolate-x interpolate check-enclosed describe]]
            [point])
  (:import [point Point]))

(deftest test-lagrange-interpolation
  (testing "Test lagrange interpolation in specified point"
    (is (= 2.0 (interpolate-x "lagrange" [(Point. 2.0 2.0)] 2.0)) "Interpolation in existing point")
    (is (= 1.5 (interpolate-x "lagrange" [(Point. 1.0 1.0) (Point. 3.0 2.0)] 2.0))))
  (testing "Test lagrange interpolation without specific point"
    (is (= '([1.2 1.2] [1.4 1.4] [1.6 1.6] [1.8 1.8] [2.0 2.0])
           (interpolate "lagrange" 0 [(Point. 2.0 2.0) (Point. 1.0 1.0)] 0.2))
        "Test correct points count and interval size")
    (is (= nil
           (interpolate "lagrange" 0 [(Point. 2.0 2.0)] 0.2))
        "Test not enough points")
    (is (= nil
           (interpolate "lagrange" 2 [(Point. 2.0 2.0) (Point. -2.0 2.0)] 0.2))
        "Test not enough points (window not enclosed the whole dataset)")
    (is (= []
           (interpolate "lagrange" 0 [(Point. 2.0 2.0) (Point. 1.0 1.0)] 2))
        "Test too big interval")))

(deftest test-linear-interpolation
  (testing "Test linear interpolation in specified point"
    (is (= 2.0 (interpolate-x "linear" [(Point. 2.0 2.0)] 2.0)) "Interpolation in existing point")
    (is (= 1.5 (interpolate-x "linear" [(Point. 1.0 1.0) (Point. 3.0 2.0)] 2.0)))))

(deftest test-enclosed-check
  (testing "Test work of enclosed checking function"
    (is (true? (check-enclosed 1.0 [(Point. 2.0 2.0)])) "Enclosed by one point - true")
    (is (true? (check-enclosed 1.0 [(Point. 1.0 1.0) (Point. 2.0 2.0)])) "Enclosed by interval with size same as window size")
    (is (false? (check-enclosed 1.0 [(Point. 4.0 1.0) (Point. 1.0 2.0)])) "Unenclosed success check")))

(deftest test-describe
  (testing "Test describing the results"
    (is (= "Interpolated by Lagrange: Not enough data." (describe "lagrange" nil)) "Null results describing (lagrange)")
    (is (= "Interpolated by Linear: Not enough data." (describe "linear" nil)) "Null results describing (linear)")
    (is (= "Interpolated by Lagrange: Not enough data." (describe "lagrange" [])) "Empty results describing")
    (is (= "Interpolated by Linear: Not enough data." (describe "linear" [])) "Empty results describing")
    (is (= "Interpolated by Lagrange: f(2.000)=2.000" (describe "lagrange" [[2.0 2.0]])) "Some results describing")
    (is (= "Interpolated by Linear: f(2.000)=2.000" (describe "linear" [[2.0 2.0]])) "Some results describing")))
