(ns huntingtrapping.advisor
  "Hunting Trapping Advisor — proposing a logistics-coordination
  operation (log a harvest record, schedule an equipment-maintenance
  operation, flag a compliance concern for human review, or coordinate
  a non-weapon supplies order) from a hunter/trapper's request.
  Swappable mock/llm; the advisor ONLY proposes —
  `huntingtrapping.governor` checks permit basis and harvest-report
  attachment independently, always escalates flagged concerns and
  above-threshold supply orders, and — this is the actor's defining
  constraint — has NO op it can ever propose that resembles firing a
  weapon, setting or springing a trap, or making a kill/harvest-timing
  decision; those verbs do not exist anywhere in this namespace or in
  `huntingtrapping.governor`'s closed op-allowlist. Modeled on
  cloud-itonami-isco-3351's customsinspection.advisor.

  A proposal: {:op :log-harvest-record|:schedule-equipment-operation|
                    :flag-compliance-concern|:coordinate-supply-order
               :effect :propose :permit-id str
               :harvest-report-attached? boolean :cost number
               :species str :quantity number :location str
               :stake kw :confidence n :rationale str}

  IMPORTANT (self-tripping-bug guardrail, see governor.cljc): default
  rationale text below is phrased around DOCUMENTATION verbs (log,
  schedule, flag, coordinate) and deliberately never uses the
  finalization/execution phrases the governor's defense-in-depth
  scope-exclusion scan looks for (\"fire the weapon\", \"spring the
  trap\", \"make the kill decision\", etc.) — governor_test.clj asserts
  this holds for every op this advisor can produce.")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- rationale-for [op operator-id permit-id]
  (case op
    :log-harvest-record
    (str "logged harvest record for operator " operator-id " under permit " permit-id
         " — post-hoc documentation only, entered after the fact, no weapon-firing,"
         " trap-setting/springing or kill/harvest-timing decision proposed or implied")

    :schedule-equipment-operation
    (str "scheduled equipment maintenance/inspection operation for operator " operator-id
         " under permit " permit-id " — logistics scheduling only")

    :flag-compliance-concern
    (str "flagged compliance concern for operator " operator-id " under permit " permit-id
         " — routed for human hunter/trapper and regulatory-authority review")

    :coordinate-supply-order
    (str "coordinated non-weapon supplies order for operator " operator-id
         " under permit " permit-id " — procurement coordination only")

    (str "proposed " (name op) " for operator " operator-id " under permit " permit-id
         " — documentation/logistics-coordination only, no weapon-firing,"
         " trap-setting/springing or kill/harvest-timing decision proposed or implied")))

(defn- infer [_store {:keys [op stake operator-id permit-id
                              harvest-report-attached? cost species quantity location]
                       :as request}]
  {:op op
   :effect :propose
   :permit-id permit-id
   :harvest-report-attached? (boolean harvest-report-attached?)
   :cost cost
   :species species
   :quantity quantity
   :location location
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (rationale-for op operator-id permit-id)})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a hunting/trapping logistics-coordination advisor. Given a
   request, propose an :op (only :log-harvest-record,
   :schedule-equipment-operation, :flag-compliance-concern or
   :coordinate-supply-order — no other op exists), the :permit-id,
   whether a harvest report is attached, a :cost when relevant, an
   honest :confidence and a :stake. You have NO authority to fire a
   weapon, set or spring a trap, or make a kill/harvest-timing
   decision, and must never phrase a proposal as if you did — those
   decisions belong solely to the human hunter/trapper. If an
   observation suggests a permit, season or bag-limit question may
   need attention, the only correct response is
   :flag-compliance-concern, which always requires human sign-off and
   is never auto-commit-eligible. The governor independently checks
   permit registration, harvest-report attachment and supply-order
   cost thresholds.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
