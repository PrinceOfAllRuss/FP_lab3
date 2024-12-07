(ns cli-test
  (:require [clojure.test :refer [deftest testing is]]
            [cli :refer [-main]]))

(deftest test-unenclosed
  (testing "Test unenclosed mode"
    (with-in-str "1;1\n3;3\n5;6\n11;2"
      (let [output (with-out-str (-main "-step" "1" "-a" "lagrange"))
            normalized-output (clojure.string/replace output #"\r\n" "\n")]
        (is (= normalized-output "-> (1.0;1.0)\nInterpolated by Lagrange: Not enough data.\n-> (1.0;1.0) (3.0;3.0)\nInterpolated by Lagrange: f(2.000)=2.000\n-> (1.0;1.0) (3.0;3.0) (5.0;6.0)\nInterpolated by Lagrange: f(4.000)=4.375\n-> (1.0;1.0) (3.0;3.0) (5.0;6.0) (11.0;2.0)\nInterpolated by Lagrange: f(6.000)=7.281; f(7.000)=8.100; f(8.000)=8.219; f(9.000)=7.400; f(10.000)=5.406\nEOF\n")
            "Test success")))
    (with-in-str "1:1"
      (is (thrown? Exception (-main "-step" "1" "-a" "lagrange"))
          "Test stop on incorrect input"))
    (with-in-str "1;1\n2;2"
      (let [output (with-out-str (-main "-step" "1" "-a" "lagrange"))
            normalized-output (clojure.string/replace output #"\r\n" "\n")]
        (is (= normalized-output "-> (1.0;1.0)\nInterpolated by Lagrange: Not enough data.\n-> (1.0;1.0) (2.0;2.0)\nInterpolated by Lagrange: Not enough data.\nEOF\n")
            "Test sampling work correctly")))
    (with-in-str "1;1\n2;2"
      (let [output (with-out-str (-main "-step" "0.2" "-a" "lagrange"))
            normalized-output (clojure.string/replace output #"\r\n" "\n")]
        (is (= normalized-output "-> (1.0;1.0)\nInterpolated by Lagrange: Not enough data.\n-> (1.0;1.0) (2.0;2.0)\nInterpolated by Lagrange: f(1.200)=1.200; f(1.400)=1.400; f(1.600)=1.600; f(1.800)=1.800; f(2.000)=2.000\nEOF\n")
            "Test sampling work correctly")))
    (with-in-str "1;1\n2;2"
      (let [output (with-out-str (-main "-step" "0.2" "-a" "lagrange,linear"))
            normalized-output (clojure.string/replace output #"\r\n" "\n")]
        (is (= normalized-output "-> (1.0;1.0)\nInterpolated by Lagrange: Not enough data.\nInterpolated by Linear: Not enough data.\n-> (1.0;1.0) (2.0;2.0)\nInterpolated by Lagrange: f(1.200)=1.200; f(1.400)=1.400; f(1.600)=1.600; f(1.800)=1.800; f(2.000)=2.000\nInterpolated by Linear: f(1.200)=1.200; f(1.400)=1.400; f(1.600)=1.600; f(1.800)=1.800; f(2.000)=2.000\nEOF\n")
            "Test two algorithms at once")))))

(deftest test-enclosed
  (testing "Test enclosed mode"
    (with-in-str "1;1\n3;3\n5;6\n11;2"
      (let [output (with-out-str (-main "-step" "1" "-a" "lagrange"))
            normalized-output (clojure.string/replace output #"\r\n" "\n")]
        (is (= normalized-output "-> (1.0;1.0)\nInterpolated by Lagrange: Not enough data.\n-> (1.0;1.0) (3.0;3.0)\nInterpolated by Lagrange: f(2.000)=2.000\n-> (1.0;1.0) (3.0;3.0) (5.0;6.0)\nInterpolated by Lagrange: f(4.000)=4.375\n-> (1.0;1.0) (3.0;3.0) (5.0;6.0) (11.0;2.0)\nInterpolated by Lagrange: f(6.000)=7.281; f(7.000)=8.100; f(8.000)=8.219; f(9.000)=7.400; f(10.000)=5.406\nEOF\n")
            "Test success")))))
