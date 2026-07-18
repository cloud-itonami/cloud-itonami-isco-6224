# Business Model: Hunting and Trapping Logistics Coordination

## Classification

- Repository: `cloud-itonami-isco-6224`
- ISCO-08: `6224`
- Occupation: Hunters and Trappers
- Social impact: wildlife-population-management, rural-livelihoods, regulatory-transparency

## Customer

- individual hunters and trappers
- hunting/trapping outfits and cooperatives

## Offer

- catch/harvest record documentation (species, quantity, location entry — after the fact)
- equipment-maintenance and inspection scheduling
- non-weapon hunting/trapping-supplies procurement coordination
- always-escalating concern flagging for human hunter/trapper review

## Scope Exclusion (not an offer)

- weapon-firing, trap-setting/springing or kill/harvest-timing-decision
  authority — this actor never performs, simulates performing, or proposes
  performing any of these; they are structurally absent from the
  op-allowlist, not a withheld feature.
- bag-limit, season and permit/licensing-compliance determinations — this
  actor never decides on the permit itself; it only routes paperwork and
  flags concerns for the human hunter/trapper and regulatory authorities to
  review and act on.

## Revenue

- monthly operator retainer
- per-record documentation fee

## Trust Controls

- no harvest record logged without an attached harvest report
- no supply order above the permit's registered cost threshold without
  governor escalation to human sign-off
- every flagged compliance concern always escalates immediately to the
  human hunter/trapper and is never auto-commit-eligible
- documentation and scheduling records are auditable, not editable
- no op, code path, or approved proposal can ever fire a weapon, set or
  spring a trap, or make a kill/harvest-timing decision
