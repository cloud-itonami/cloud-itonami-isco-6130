(ns mixed-farming.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [mixed-farming.actor :as actor]
            [mixed-farming.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-plot! st {:plot-id "plot-1" :has-livestock? false :near-water? false})
    (store/register-plot! st {:plot-id "plot-2" :has-livestock? true :near-water? false})
    st))

(deftest commits-a-clean-low-risk-request
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:plot-id "plot-1" :op :harvest :stake :low}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "plot-1"))))))

(deftest holds-on-unregistered-plot-without-committing
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:plot-id "no-such-plot" :op :harvest :stake :low}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :done (:status result)))
    (is (nil? (get-in result [:state :record])))
    (is (empty? (store/records-of st "no-such-plot")))
    (is (= :hold (:disposition (:state result))))))

(deftest interrupts-then-commits-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        ;; livestock-medication on plot-2 always escalates (governor invariant)
        request {:plot-id "plot-2" :op :medicate-livestock :stake :medium}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "plot-2")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (some? (get-in resumed [:state :record])))
      (is (= 1 (count (store/records-of st "plot-2")))))))
