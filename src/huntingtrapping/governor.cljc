(ns huntingtrapping.governor
  "HuntingTrappingGovernor — the independent safety/traceability layer
  gating every logistics-coordination proposal an advisor may make for
  a hunter/trapper. The governor never dispatches hardware itself and
  never exercises, simulates exercising, or approves exercising ANY
  decision to fire a weapon, set/spring a trap, or make a
  kill/harvest-timing decision. Modeled on cloud-itonami-isco-3351's
  customsinspection.governor.

  == Why this actor cannot become a weapon-firing / trap-setting /
     kill-decision authority (structural, not merely gated) ==

  Real hunters and trappers exercise firearms/traps and directly kill
  or capture wild animals — that is their domain's actual physical
  work. This actor is a logistics/record-keeping coordination robot
  ONLY — it helps log catch/harvest records (post-hoc metadata, never
  a kill/harvest-timing decision), schedule equipment maintenance, and
  route permit/licensing paperwork (never decides on the permit
  itself; bag-limit, season and permit-validity determinations belong
  to the human hunter/trapper and regulatory authorities). It is
  deliberately built so that weapon-firing/trap-setting/kill-decision
  authority can never leak in through two independent layers:

    Layer 1 — closed op-allowlist (`op-allowlist`, rule :unknown-op).
      The only ops this governor will ever accept are
      `:log-harvest-record`, `:schedule-equipment-operation`,
      `:flag-compliance-concern` and `:coordinate-supply-order`. No op
      resembling firing a weapon, setting/springing a trap, or making
      a kill/harvest-timing decision exists anywhere in this
      codebase's vocabulary — not as a gated/escalated op, not as a
      `:hold`-by-default op, not at all. Any op keyword outside this
      four-op set — including a hypothetical `:fire-weapon` or
      `:spring-trap` a buggy or malicious caller might construct
      directly, bypassing the advisor entirely — is unconditionally
      hard-blocked here. This is the primary structural guarantee: the
      allowlist has nothing to point at.

    Layer 2 — defense-in-depth textual scope-exclusion
      (`hunting-execution-scope-exclusion-phrases`, rule
      :hunting-execution-scope-exclusion). Even though no op can
      express a weapon-firing/trap-setting/kill-decision action, this
      governor also hard-blocks any proposal whose free-text
      `:rationale` claims to finalize or execute one of those actions
      (\"fire the weapon\", \"spring the trap\", \"set the trap\",
      \"make the kill decision\", \"make the harvest-timing
      decision\"). This is deliberately phrased as
      finalization/execution ACTION PHRASES, never bare nouns like
      \"weapon\" or \"trap\" — a bare-noun list would false-trip on
      completely routine documentation rationale (e.g. this actor's
      own default mock-advisor text describing that NO weapon-firing
      action is proposed necessarily mentions the word \"weapon\").
      See advisor.cljc's docstring and the
      `default-mock-advisor-proposals-never-self-trip-scope-exclusion`
      test in governor_test.clj, which pins this down for every op the
      mock advisor can produce.

  Any observation this actor's robot logs that MAY warrant regulatory
  attention (permit/season/bag-limit questions) is surfaced ONLY via
  the always-escalating `:flag-compliance-concern` op — reviewed and
  acted on by the human hunter/trapper themselves. The robot never
  acts on it, not even as a `:propose`.

  HARD invariants (:hard? true, ALWAYS :hold, permanent,
  un-overridable):
    1. operator provenance     — the hunter/trapper must be
                                  independently verified/registered.
    2. no-actuation             — proposal :effect must be :propose
                                  (the governor never dispatches
                                  hardware and never itself fires a
                                  weapon, sets/springs a trap, or makes
                                  a kill/harvest-timing decision; it
                                  only gates what the advisor may
                                  log/schedule/flag/order).
    3. closed op-allowlist      — see Layer 1 above (:unknown-op).
    4. hunting-execution scope exclusion — see Layer 2 above
                                  (:hunting-execution-scope-exclusion).
    5. permit basis              — a permit-scoped proposal must cite
                                  a REGISTERED permit belonging to
                                  this operator.
    6. harvest report attached  — a `:log-harvest-record` proposal
                                  must have
                                  `:harvest-report-attached?` true
                                  (a post-hoc field report/log basis)
                                  before it can be logged (logging a
                                  record with no attached harvest
                                  report is a fabricated record, not
                                  documentation).
  ESCALATION invariants (:escalate? true, ALWAYS human hunter/trapper
  sign-off per business-model.md's Trust Controls — these are
  :high/:safety-critical regardless of confidence):
    7. :op :flag-compliance-concern — always escalates immediately and
       is never auto-commit-eligible; the human hunter/trapper reviews
       and decides whether any further action (which this actor cannot
       itself take) is warranted.
    8. :coordinate-supply-order whose :cost exceeds the permit's
       registered `:max-supply-order-cost`.
    9. low confidence (< `confidence-floor`)."
  (:require [huntingtrapping.store :as store]
            [kotoba.lang.text :as str]))

(def confidence-floor 0.6)

(def ^:private op-allowlist
  #{:log-harvest-record :schedule-equipment-operation
    :flag-compliance-concern :coordinate-supply-order})

(def ^:private always-escalate-ops #{:flag-compliance-concern})

;; Deliberately full finalization/execution ACTION PHRASES, never bare
;; nouns — see the namespace docstring's "Layer 2" explanation and the
;; self-tripping-bug guardrail this exists to avoid.
(def ^:private hunting-execution-scope-exclusion-phrases
  ["fire the weapon" "fired the weapon" "fires the weapon" "firing the weapon"
   "fire a weapon" "fired a weapon" "fires a weapon" "firing a weapon"
   "discharge the weapon" "discharged the weapon" "discharges the weapon" "discharging the weapon"
   "discharge a weapon" "discharged a weapon" "discharges a weapon" "discharging a weapon"
   "set the trap" "sets the trap" "setting the trap"
   "set a trap" "sets a trap" "setting a trap"
   "spring the trap" "sprang the trap" "springs the trap" "springing the trap" "sprung the trap"
   "spring a trap" "sprang a trap" "springs a trap" "springing a trap"
   "make the kill decision" "made the kill decision" "makes the kill decision" "making the kill decision"
   "make the harvest-timing decision" "made the harvest-timing decision"
   "makes the harvest-timing decision" "making the harvest-timing decision"])

(defn- scope-excluded-text? [s]
  (let [s (str/lower (or s ""))]
    (boolean (some #(str/includes? s %) hunting-execution-scope-exclusion-phrases))))

(defn- needs-permit? [op]
  (contains? op-allowlist op))

(defn- hard-violations [{:keys [request proposal]} operator-record p]
  (let [{:keys [op permit-id harvest-report-attached? rationale]} proposal
        log? (= :log-harvest-record op)]
    (cond-> []
      (nil? operator-record)
      (conj {:rule :no-operator :detail "未登録 operator — provenance が独立に検証されていない"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation
             :detail "effect は :propose のみ許可（governor は発砲・罠の設置/作動を一切直接実行しない）"})

      (not (contains? op-allowlist op))
      (conj {:rule :unknown-op
             :detail (str "許可されていない op: " (pr-str op)
                          "（発砲・罠の設置/作動・捕殺/収穫タイミングの判断に"
                          "相当する op は closed allowlist に一切存在しない）")})

      (scope-excluded-text? rationale)
      (conj {:rule :hunting-execution-scope-exclusion
             :detail (str "提案文言が発砲・罠の設置/作動・捕殺/収穫タイミングの判断を確定・実行する"
                          "記述を含む — たとえ :propose であっても恒久的に禁止（allowlistに"
                          "存在しない権限を文言で回避することを防ぐ多層防御）")})

      (and (needs-permit? op) (nil? permit-id))
      (conj {:rule :missing-permit :detail "permit-id が未指定"})

      (and (needs-permit? op) permit-id (nil? p))
      (conj {:rule :unknown-permit :detail "未登録 permit への提案は不可"})

      (and (needs-permit? op) permit-id p
           (not= (:operator-id p) (:operator-id request)))
      (conj {:rule :permit-wrong-operator :detail "permit が別 operator の管轄"})

      (and log? (not harvest-report-attached?))
      (conj {:rule :missing-harvest-report
             :detail "原始記録（フィールドレポート等）が添付されていない記録の記入は捏造記録であって文書化ではない"}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `huntingtrapping.store/Store`. Pure — never
  mutates the store, never fires a weapon, sets/springs a trap, or
  makes a kill/harvest-timing decision."
  [request context proposal store]
  (let [operator-record (store/operator store (:operator-id request))
        p (some->> (:permit-id proposal) (store/permit store))
        hard (hard-violations {:request request :proposal proposal} operator-record p)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        over-cost? (and (= :coordinate-supply-order (:op proposal))
                        p (number? (:cost proposal))
                        (number? (:max-supply-order-cost p))
                        (> (:cost proposal) (:max-supply-order-cost p)))
        always-risky? (or (contains? always-escalate-ops (:op proposal)) over-cost?)]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
