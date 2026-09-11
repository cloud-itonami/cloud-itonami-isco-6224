(ns huntingtrapping.store
  "SSoT for the ISCO-08 6224 hunting/trapping logistics-coordination
  actor (itonami actor pattern, ADR-2607121000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a logistics-coordination
  robot performs catch/harvest record-keeping, equipment-maintenance
  scheduling and non-weapon supplies procurement paperwork for a
  hunter/trapper under this advisor/governor pair, which never
  dispatches hardware itself and never exercises, simulates
  exercising, or proposes exercising ANY decision to fire a weapon,
  set/spring a trap, or make a kill/harvest-timing decision — that
  authority is not merely gated, it is structurally absent from this
  actor's op-allowlist; see huntingtrapping.governor). Modeled on
  cloud-itonami-isco-3351's customsinspection.store (two-layer
  op-allowlist + text-scan exclusion pattern) and
  cloud-itonami-isco-7111's housebuilder.store (module shape).

  Domain:

    operator — a registered, independently-verified hunter/trapper
               (:operator-id :name). Analogous to customsinspection's
               `inspector` — provenance must be established before any
               proposal for this operator can be considered.
    permit   — a registered hunting/trapping permit or license
               {:permit-id :operator-id :name :max-supply-order-cost}.
               `:max-supply-order-cost` is the registered threshold a
               proposed `:coordinate-supply-order` cost must not
               exceed without escalating to human sign-off (NOT a hard
               block — procurement above threshold is legitimate,
               routine administrative work that simply requires the
               human hunter/trapper's approval, unlike this actor's
               complete lack of any op resembling a weapon-firing,
               trap-setting/springing or kill/harvest-timing decision).
               Bag-limit, season and permit-validity determinations
               belong to the human hunter/trapper and regulatory
               authorities — this actor never decides on the permit
               itself, it only cites a permit's registration as a
               provenance basis for routing paperwork.
    record   — a committed operating record (a logged harvest entry,
               an equipment-maintenance schedule, a flagged compliance
               concern, or a supply order) — written ONLY via
               commit-record!. Never a kill record, a trap-deployment
               record or a weapon-discharge record — no such record
               type exists because no such op exists.
    ledger   — append-only audit trail, commit or hold.")

(defprotocol Store
  (operator [s operator-id])
  (permit [s permit-id])
  (records-of [s operator-id])
  (ledger [s])
  (register-operator! [s op])
  (register-permit! [s p])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (operator [_ operator-id] (get-in @a [:operators operator-id]))
  (permit [_ permit-id] (get-in @a [:permits permit-id]))
  (records-of [_ operator-id] (filter #(= operator-id (:operator-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-operator! [s op]
    (swap! a assoc-in [:operators (:operator-id op)] op) s)
  (register-permit! [s p]
    (swap! a assoc-in [:permits (:permit-id p)] p) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:operators {} :permits {} :records [] :ledger []}
                                    seed)))))
