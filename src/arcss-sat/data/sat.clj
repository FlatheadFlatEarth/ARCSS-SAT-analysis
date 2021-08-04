(ns arcss-sat.data.sat
  (:require [clojure.data.csv :as csv]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [incanter.core :as incanter]
            [incanter.charts :as i-charts])
  (:import [java.time LocalDateTime ZoneOffset]
           [java.time.chrono ChronoLocalDateTime]))

(defonce guide (slurp "resources/data/SAT/SAT_GRID.txt"))

(defonce data-string (slurp "resources/data/SAT/SAT_2008.txt"))

(defn abridge-data [v]
  (concat []
          (take 5 v)
          (take-last 5 v)))

(defn shared-parse-raw-row [s]
  (->> (str/split s #" ")
       (map str/trim)
       (remove #(or (= "" %) (nil? %)))
       (map edn/read-string)))

(defn parse-guide-row [raw-row]
  (let [[lat lon] (shared-parse-raw-row raw-row)]
    {:lat lat
     :lon lon}))

(defn get-coords []
  (let [all-coords (->> (str/split guide #"\n")
                        rest
                        (map parse-guide-row))]
    all-coords))

(defonce abridged-coords (abridge-data (get-coords)))

(defn parse-temp-row [raw-row]
  (let [[y m d h & temps] (shared-parse-raw-row raw-row)]
    {:date (LocalDateTime/of y m d h 0)
     :temps (map (fn [temp {:keys [lat lon] :as loc} idx]
                   {:temp temp
                    :lat lat
                    :lon lon
                    :pos/idx idx})
                 (abridge-data temps)
                 abridged-coords
                 (range))}))


(defonce temp-records (->> (str/split data-string #"\n")
                           (map parse-temp-row)))

(defn filter-temps [n]
  (for [{:keys [date temps]} temp-records]
    (->> (filter (fn [{:keys [pos/idx]}]
                   (= idx n))
                 temps)
         first
         :temp)))

(defn generate-time-and-temps []
  (let [zone-offset ZoneOffset/UTC
        x (map (fn [{:keys [date]}]
                 (* (.toEpochSecond date zone-offset)
                    1000))
               temp-records)
        north-pole (filter-temps 0)
        south-pole (filter-temps 9)]
    [x
     north-pole
     south-pole]))

(defn make-export-dataset []
  (let [[_ north-pole south-pole] (generate-time-and-temps)
        dates (map :date temp-records)]
    (map (fn [d n s]
           [d n s])
         dates
         north-pole
         south-pole)))

(defn make-dataset []
  (let [[dates north-pole south-pole] (generate-time-and-temps)
        data (incanter/dataset ["Date"
                                "North Pole Temp"
                                "South Pole Temp"]
                               (map (fn [d n s]
                                      [d n s])
                                    dates
                                    north-pole
                                    south-pole))]
    data))

(defn table-temps []
  (let [data (make-dataset)
        table (incanter/data-table data)]
     (incanter/view table)))

(defn chart-temps [& {:keys [save-image?]}]
  (let [[dates north-pole south-pole] (generate-time-and-temps)
        chart1 (i-charts/time-series-plot dates north-pole
                                          :x-label "Date"
                                          :y-label "Temp (C)")
        _ (i-charts/add-lines chart1      dates south-pole)
        cw1 (incanter/view chart1)]
    (when save-image?
      (incanter/save chart1 "out/chart.png"))
    cw1))

(defn export-temps []
  (let [data (make-export-dataset)
        title-row ["Date"
                   "North Pole Temp (C)"
                   "South Pole Temp (C)"]
        data (concat [title-row] data)]
    (with-open [writer (io/writer "out/temperature_data.csv")]
      (csv/write-csv writer data))))

(comment
  (chart-temps :save-image? true)
  (chart-temps)

  (export-temps)

  (table-temps))
