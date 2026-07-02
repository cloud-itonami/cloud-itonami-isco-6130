(ns mixed-farming.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [mixed-farming.store :as store]
            [mixed-farming.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-plot! st {:plot-id "plot-1" :has-livestock? false :near-water? false})
    (store/register-plot! st {:plot-id "plot-2" :has-livestock? true :near-water? false})
    (store/register-plot! st {:plot-id "plot-3" :has-livestock? false :near-water? true})
    st))

(deftest ok-on-clean-harvest
  (let [st (fresh-store)
        proposal {:op :harvest :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:plot-id "plot-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-plot
  (let [st (fresh-store)
        proposal {:op :harvest :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:plot-id "no-such-plot"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-plot (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        proposal {:op :harvest :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:plot-id "plot-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-chemical-near-water
  (let [st (fresh-store)
        proposal {:op :treat-crop :effect :propose :confidence 0.9 :stake :medium}
        v (governor/check {:plot-id "plot-3"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-livestock-medication
  (let [st (fresh-store)
        proposal {:op :medicate-livestock :effect :propose :confidence 0.9 :stake :medium}
        v (governor/check {:plot-id "plot-2"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :harvest :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:plot-id "plot-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:plot-id "plot-1" :op :harvest})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/records-of st "plot-1"))))
    (is (= 1 (count (store/ledger st))))))
