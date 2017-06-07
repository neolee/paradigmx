(ns paradigmx.handler
  (:require [clojure.tools.logging :as log]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [ring.middleware.cors :refer [wrap-cors]]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.predicates :as pr]
            [clj-time.periodic :as p]))

(def holidays {"2017" #{"0101", "0102"
                        "0127", "0128", "0129", "0130", "0131", "0201", "0202",
                        "0402", "0403", "0404",
                        "0501",
                        "0528", "0529", "0530",
                        "1001", "1002", "1003", "1004", "1005", "1006", "1007", "1008"}})

(def rotations {"2017" #{"0122", "0204",
                         "0401",
                         "0527",
                         "0930"}})

(defn rotation?
  "test given date return true if it's a rotation work day"
  [d]
  (let [y (subs d 0 4) md (subs d 4 8)]
    (if (contains? (rotations y) md)
      true
      false))
  )

(defn weekend?
  "test given date return true if it's a weekend day"
  [d]
  (pr/weekend? (f/parse (f/formatter "yyyyMMdd") d))
  )

(defn holiday?
  "test given date return true if it's a holiday"
  [d]
  (let [y (subs d 0 4) md (subs d 4 8)]
    (if (contains? (holidays y) md)
      true
      (if (and (weekend? d) (not (rotation? d)))
        true
        false)))
  )

(defn holidays-in-month
  "return all holidays in given month"
  [month]
  (let [m (f/parse (f/formatter "yyyyMM") month)
        d1 (t/first-day-of-the-month m)
        d2 (t/last-day-of-the-month m)
        n (+ (t/in-days (t/interval d1 d2)) 1)
        days (map #(f/unparse (f/formatter "yyyyMMdd") %) (take n (p/periodic-seq d1 (t/days 1))))
        ]
    (filter holiday? days)
    )
  )

(def app
  (->
   (api
    {:swagger
     {:ui "/"
      :spec "/swagger.json"
      :data {:info {:title "Paradigm X"
                    :description "Open APIs for Paradigm X micro-services"}
             :tags [{:name "holiday", :description "Holiday data services"}]}}}
    (context "/holiday" []
      :tags ["holiday"]
      (GET "/check" []
        :return {:isHoliday Boolean}
        :query-params [date :- String]
        :summary "test given date return true if it's a holiday"
        (ok {:isHoliday (holiday? date)}))
      (GET "/holidays" []
        :return {:holidays [String]}
        :query-params [month :- String]
        :summary "return all holidays in given month"
        (ok {:holidays (holidays-in-month month)}))))
   (wrap-cors :access-control-allow-origin #".*"
              :access-control-allow-methods [:get :put :post]
              :access-control-allow-headers ["Content-Type"])))