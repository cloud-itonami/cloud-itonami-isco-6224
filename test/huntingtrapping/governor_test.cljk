(ns huntingtrapping.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [huntingtrapping.store :as store]
            [huntingtrapping.advisor :as advisor]
            [huntingtrapping.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-operator! st {:operator-id "operator-1" :name "R. Okafor"})
    (store/register-permit! st {:permit-id "P-1" :operator-id "operator-1"
                                :name "trapping-license-2026"
                                :max-supply-order-cost 5000})
    st))

(defn- log-op [attached?]
  {:op :log-harvest-record :effect :propose :permit-id "P-1"
   :harvest-report-attached? attached?
   :confidence 0.9 :stake :low})

(defn- supply-op [cost]
  {:op :coordinate-supply-order :effect :propose :permit-id "P-1"
   :cost cost :confidence 0.9 :stake :low})

(def ^:private req {:operator-id "operator-1"})

;; -- routine documentation ops: ok/hard on permit + harvest-report basis --

(deftest ok-log-harvest-record-with-attached-harvest-report
  (let [st (fresh-store)
        v (governor/check req {} (log-op true) st)]
    (is (:ok? v))))

(deftest hard-on-missing-harvest-report
  (testing "logging a record without an attached harvest report is a fabricated record, not documentation"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (log-op false) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :missing-harvest-report (:rule %)) (:violations v))))))

(deftest hard-on-unknown-permit
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op true) :permit-id "P-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-permit (:rule %)) (:violations v)))))

(deftest hard-on-permit-wrong-operator
  (let [st (fresh-store)]
    (store/register-operator! st {:operator-id "operator-2" :name "Other"})
    (let [v (governor/check {:operator-id "operator-2"} {} (log-op true) st)]
      (is (:hard? v))
      (is (some #(= :permit-wrong-operator (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-operator
  (let [st (fresh-store)
        v (governor/check {:operator-id "nobody"} {} (log-op true) st)]
    (is (:hard? v))
    (is (some #(= :no-operator (:rule %)) (:violations v)))))

(deftest hard-on-missing-permit-id
  (let [st (fresh-store)
        v (governor/check req {} (dissoc (log-op true) :permit-id) st)]
    (is (:hard? v))
    (is (some #(= :missing-permit (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op true) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

;; -- structural absence of weapon-firing/trap-setting/kill-decision authority --
;; No such op exists in the advisor's vocabulary, but a buggy or
;; malicious caller could still hand the governor a proposal map
;; directly (bypassing the advisor). Both layers must independently
;; hard-block that, permanently, never overridable via approval.

(deftest hard-on-op-outside-closed-allowlist
  (testing "no op resembling weapon-firing/trap-setting/kill-decision exists in the allowlist"
    (doseq [phantom-op [:fire-weapon :discharge-weapon :set-trap :spring-trap
                        :make-kill-decision :make-harvest-timing-decision]]
      (let [st (fresh-store)
            v (governor/check req {} (assoc (log-op true) :op phantom-op :confidence 0.99) st)]
        (is (:hard? v) (str phantom-op " must hard-block"))
        (is (some #(= :unknown-op (:rule %)) (:violations v)) (str phantom-op " must trip :unknown-op"))))))

(deftest hard-on-hunting-execution-scope-exclusion-text
  (testing "a proposal's free text cannot finalize/execute a weapon-firing/trap-setting/kill-decision action even under a routine op"
    (doseq [phrase ["we will fire the weapon at the target"
                    "recommend we spring the trap now"
                    "propose to set the trap at the north line"
                    "advise we make the kill decision"
                    "will make the harvest-timing decision for this animal"]]
      (let [st (fresh-store)
            v (governor/check req {} (assoc (log-op true) :rationale phrase :confidence 0.99) st)]
        (is (:hard? v) (str "must hard-block: " phrase))
        (is (some #(= :hunting-execution-scope-exclusion (:rule %)) (:violations v))
            (str "must trip scope-exclusion: " phrase))))))

(deftest scope-exclusion-does-not-false-trip-on-bare-nouns
  (testing "bare nouns describing what was NOT done must not self-trip (only finalization/execution phrases do)"
    (let [st (fresh-store)
          v (governor/check req {}
                             (assoc (log-op true)
                                    :rationale "no weapon-firing, trap-setting/springing or kill/harvest-timing decision proposed or implied")
                             st)]
      (is (not (some #(= :hunting-execution-scope-exclusion (:rule %)) (:violations v)))))))

;; -- default mock-advisor proposals must never self-trip the governor --
;; This is the dedicated regression test CLAUDE.md calls for: the
;; governor's own scope-exclusion term list must never accidentally
;; match inside the mock advisor's own default rationale text.

(deftest default-mock-advisor-proposals-never-self-trip-scope-exclusion
  (let [st (fresh-store)
        mock (advisor/mock-advisor)
        requests [{:op :log-harvest-record :operator-id "operator-1" :permit-id "P-1"
                   :harvest-report-attached? true :stake :low}
                  {:op :schedule-equipment-operation :operator-id "operator-1" :permit-id "P-1"
                   :stake :low}
                  {:op :flag-compliance-concern :operator-id "operator-1" :permit-id "P-1"
                   :stake :low}
                  {:op :coordinate-supply-order :operator-id "operator-1" :permit-id "P-1"
                   :cost 1000 :stake :low}]]
    (doseq [request requests]
      (let [proposal (advisor/-advise mock st request)
            v (governor/check req {} proposal st)]
        (is (not (some #(= :hunting-execution-scope-exclusion (:rule %)) (:violations v)))
            (str "op " (:op request) " default rationale self-tripped: " (:rationale proposal)))
        (is (not (some #(= :unknown-op (:rule %)) (:violations v)))
            (str "op " (:op request) " unexpectedly rejected by closed allowlist"))))))

;; -- always-escalate ops / thresholds --

(deftest always-escalates-flag-compliance-concern-even-at-high-confidence
  (testing "an observation that MAY warrant permit/season/bag-limit review is NEVER auto-commit-eligible"
    (let [st (fresh-store)
          v (governor/check req {} {:op :flag-compliance-concern :effect :propose
                                    :permit-id "P-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v))
      (is (not (:ok? v))))))

(deftest escalates-supply-order-above-cost-threshold-even-at-high-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (supply-op 50000) :confidence 0.99) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest ok-supply-order-at-exact-threshold-boundary
  (testing "the supply-order cost threshold is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (supply-op 5000) st)]
      (is (:ok? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (log-op true) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))

(deftest ok-schedule-equipment-operation
  (let [st (fresh-store)
        v (governor/check req {} {:op :schedule-equipment-operation :effect :propose
                                  :permit-id "P-1" :confidence 0.9 :stake :low} st)]
    (is (:ok? v))))
