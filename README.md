# Лабораторная работа №3

```
Выполнил: Филатов Фёдор P3322
```

## Требования к разрабатываемой программе

1. Программа должна работать в потоковом режиме
2. Программа должа реализовывать несколько алгоритмов интерполяции 
3. Обязательно должна быть реализована интерполяция отрезками
4. Программа должна позволять пользователю конфигурировать ее работу при поможи аргументов
5. Данные берутся из стандартного входа и подаются на стандартный выход

## Реализация

### Реализация структуры для хранения точек
``` ./src/point.clj ```
Тесты
``` ./test/point_test.clj ```

Для работы с точками была создана структура данных Point, а также методы для сереализации и дисереализации

```clojure
(defrecord Point [x y])

(defn to-string [^Point point]
   (str "(" (:x point) ";" (:y point) ")"))

(defn parse-from-line [line]
   (let [split (split line #";")]
      (if (= (count split) 2)
         (->Point (parse-double (first split)) (parse-double (second split)))
         (throw (Exception. "Bad line provided")))))
```

### Реализация чтения в потоковом режиме
``` ./src/reader.clj ```
Тесты
``` ./test/reader_test.clj ```

Был написан метод read, для построчного чтения данных из стандартного ввода и преобразования их в рекорд Point

```clojure
(defn read []
   (let [line (try (read-line)
                   (catch Exception _ nil))]
      (if (nil? line)
         nil
         (parse-from-line line))))
```

### Реализация методов интерполяции
``` ./src/methods.clj ```
Тесты
``` ./test/methods_test.clj ```

В рамках работы были реализованы два метода

Метод интерполяции отрезками

```clojure

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

```

Метод интерполяции полиномом лагранжа

```clojure

(defn- calc-polynomial-for-point [point points target]
  (reduce (fn [acc cur]
            (if (= point cur)
              acc
              (* acc (/ (- target (:x cur)) (- (:x point) (:x cur)))))) 1 points))

(defmethod interpolate-x "lagrange" [_ points target]
  (reduce (fn [acc point]
            (let [poly (calc-polynomial-for-point point points target)]
              (+ acc (* (:y point) poly)))) 0 points))

```

Данные методы вычисляют значение функции в конкретной точки, для работы алгоритма без указания конкретных точек, 
а вычисления всех согласно частоте дискретизации был написан метод обертка 

```clojure

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

```

В рамках данного метода строится последовательность X, для которой надо вычислить значение функции 
на основе полученных от пользователя данных.

Программа также позволяет в своих аргументах задать окно, в рамках которого она будет брать точки. 
Если окно было задано пользователем (не 0), то программа перед началом применения методов 
интерполяции отфильтрует точки при помощи метода check-enclosed

```clojure

(defn check-enclosed [window-size points]
  (or (zero? window-size) (zero? (count points)) (<= (- (:x (first points)) (:x (last points))) window-size)))

```

### Реализация CLI
``` ./src/cli.clj ```
Тесты
``` ./test/cli_test.clj ```

Для работы с командной строкой была добавлена структура Command и при помощи нее описаны доступные пользователю аргументы 
```clojure

(defrecord Command [name usage docstring parse default required?])

(def options {"--help"    (->Command "help" "--help" "Display help message"
                                     nil nil false)
              "-enclosed" (->Command "enclosed" "-enclosed WINDOW" "Creates a window of the specified size and passes it through the data set. When WINDOW = 0, the general mode is used."
                                     #(parse-double %) "0" false)
              "-step"     (->Command "step" "-step X" "The value of the step x for finding the function in (f(x0 + x) = ?)"
                                         #(parse-double %) "1" false)
              "-a" (->Command "a" "-a algorithm1, algorithm2" "Interpolation algorithm(s) (\"lagrange\", \"linear\")"
                              #(map trim (split % #",")) ["lagrange"] false)})
```

Для обработки полученных от пользователя аргументов был добавлен метод parse-options

```clojure
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
                            (throw (Exception. "Bad args")))
                          [k ((:parse option) v)])) opts))
        (catch Exception _ nil)))))
```

После обработки аргументов запускается условно бесконечная рекурсия, которая последовательно читает точки 
и пытается вычислить значения в промежутках между ними согласно конфигурации переданной пользователем
```clojure
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
                          (println (describe algo (interpolate algo (get opts "--enclosed") list (get opts "-s")))))
                        (recur list))))) '())))))
```

## Примеры запуска программы

#### Help - доступные аргументы
```cmd
PS D:\Education\3_year\FP\lab3> cat ./interpolation.test | clj -M -m cli --help              
Usage: cli <args>
Available arguments:
help: --help, Display help message
enclosed: -enclosed WINDOW, Creates a window of the specified size and passes it through the data set. When WINDOW = 0, the general mode is used. -- Default: 0
step: -step X, The value of the step x for finding the function in (f(x0 + x) = ?) -- Default: 1
a: -a algorithm1, algorithm2, Interpolation algorithm(s) ("lagrange", "linear") -- Default: ["lagrange"]
```


#### Запуск с интервалом 0.5 без ограничения в окне
Ввод:
```text
1;6
2;4
5;-6
7;52
```
Результат:
```cmd
PS D:\Education\3_year\FP\lab3> cat ./interpolation.test | clj -M -m cli -step 0.5 -a lagrange             
-> (1.0;6.0)
Interpolated by Lagrange: Not enough data.
-> (1.0;6.0) (2.0;4.0)
Interpolated by Lagrange: f(1.500)=5.000
-> (1.0;6.0) (2.0;4.0) (5.0;-6.0)
Interpolated by Lagrange: f(2.500)=2.750; f(3.000)=1.333; f(3.500)=-0.250; f(4.000)=-2.000; f(4.500)=-3.917
-> (1.0;6.0) (2.0;4.0) (5.0;-6.0) (7.0;52.0)
Interpolated by Lagrange: f(5.500)=0.675; f(6.000)=12.000; f(6.500)=28.825
EOF
```


#### Запуск с интервалом 0.5 с ограничением в окне на 2
Ввод:
```text
1;6
2;4
5;-6
7;52
```
Результат:
```cmd
PS D:\Education\3_year\FP\lab3> cat ./interpolation.test | clj -M -m cli -enclosed 2 -step 0.5 -a lagrange
-> (1.0;6.0)
Interpolated by Lagrange: Not enough data.
-> (1.0;6.0) (2.0;4.0)
Interpolated by Lagrange: f(1.500)=5.000
-> (1.0;6.0) (2.0;4.0) (5.0;-6.0)
Interpolated by Lagrange: Not enough data.
-> (1.0;6.0) (2.0;4.0) (5.0;-6.0) (7.0;52.0)
Interpolated by Lagrange: f(5.500)=8.500; f(6.000)=23.000; f(6.500)=37.500
EOF
```

## Вывод

В ходе выполнения лабораторной работы были достигнуты следующие результаты:

1. Реализована программа для интерполяции данных с поддержкой:
    - Линейной интерполяции
    - Интерполяции методом Лагранжа
    - Потоковой обработки входных данных
    - Настраиваемой частоты дискретизации

2. Получены практические навыки:
    - Работы с потоковым вводом/выводом в Clojure
    - Обработки аргументов командной строки
    - Реализации алгоритмов интерполяции в функциональном стиле