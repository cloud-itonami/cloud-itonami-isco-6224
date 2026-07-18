# Operator Guide

## First Deployment

1. Define the hunter/trapper's service area and intake process.
2. Register each operator and permit (provenance basis for every proposal).
3. Run synthetic operating cases.
4. Enable human-reviewed sign-off for flagged concerns and above-threshold
   supply orders.
5. Measure operating outcomes and audit coverage.

## Minimum Production Controls

- provenance for all operating records (registered operator + permit)
- harvest-report basis for every logged harvest record
- always-escalate path for flagged compliance concerns
- human review for above-threshold supply orders
- audit export for all gated actions

## What This Actor Never Does

This actor and its operators must never configure, extend or fork it to:

- fire a weapon
- set or spring a trap
- make a kill or harvest-timing decision
- decide on bag-limit, season or permit-validity questions

These are decisions that belong solely to the human hunter/trapper (and, for
regulatory questions, the relevant regulatory authority). This actor is a
logistics/record-keeping coordination robot only — any observation
suggesting a permit/season/bag-limit question may need attention is
surfaced solely via `:flag-compliance-concern`, which always escalates
immediately to the human hunter/trapper for review and action. The robot
never acts on it.

## Certification

Certified operators must prove that the governor gates every proposal, that
flagged concerns and above-threshold supply orders escalate to the human
hunter/trapper, and that no fork has reintroduced an op or rationale path
resembling weapon-firing, trap-setting/springing or a kill/harvest-timing
decision.
