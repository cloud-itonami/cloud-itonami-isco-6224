# cloud-itonami-isco-6224

Open Occupation Blueprint for **ISCO-08 6224**: Hunters and Trappers.

This repository designs a forkable OSS business for hunting/trapping
logistics coordination: a logistics-coordination robot manages
catch/harvest records, equipment-maintenance schedules and
non-weapon-supplies procurement paperwork under a governor-gated
actor, so a hunter/trapper keeps their own operating records instead
of renting a closed wildlife-operations SaaS.

**This actor has no weapon-firing, trap-setting/springing or
kill/harvest-timing-decision authority, structurally, not merely by
policy gate.** Real hunters and trappers exercise firearms/traps and
directly kill or capture wild animals — that is their domain's actual
physical work. This actor is a logistics/record-keeping coordination
robot ONLY. Its op-allowlist has exactly four ops —
`:log-harvest-record`, `:schedule-equipment-operation`,
`:flag-compliance-concern`, `:coordinate-supply-order` — and no op
resembling firing a weapon, setting or springing a trap, or making a
kill/harvest-timing decision exists anywhere in the codebase. That
absence is enforced twice, independently: a closed op-allowlist that
hard-blocks any op outside the four above, and a defense-in-depth text
scan that hard-blocks any proposal whose rationale claims to finalize
or execute one of those actions. See
[`src/huntingtrapping/governor.cljc`](src/huntingtrapping/governor.cljc)'s
namespace docstring for the full structural argument. Any observation
that MAY warrant permit/season/bag-limit attention is surfaced ONLY
via `:flag-compliance-concern`, which always escalates immediately to
the human hunter/trapper and is never auto-commit-eligible — the robot
never acts on it itself, not even as a `:propose`. Bag-limit, season
and permit decisions belong to the human hunter/trapper and regulatory
authorities, never this actor.

**Maturity: `:implemented`.** `src/huntingtrapping/` implements the
`HuntingTrappingActor` as a `langgraph.graph/state-graph`
(`huntingtrapping.actor`) wired to a `Hunting Trapping Advisor`
(`huntingtrapping.advisor`) and an independent
`HuntingTrappingGovernor` (`huntingtrapping.governor`), following the
itonami actor pattern (ADR-2607011000 / ADR-2607121000): `:intake ->
:advise -> :govern -> :decide -+-> :commit (:ok?) +->
:request-approval (:escalate?, human-in-the-loop interrupt) +-> :hold
(:hard?)`. HARD invariants (always hold, never overridable): operator
provenance, no-actuation (`:effect` must be `:propose`), a closed
op-allowlist with no op resembling weapon-firing/trap-setting/kill
decisions, a defense-in-depth text scan blocking any rationale that
finalizes or executes such an action, a registered permit basis for
any permit-scoped proposal, and an attached harvest report before any
harvest record can be logged (logging a record without one is a
fabricated record, not documentation). Always-escalate ops (human
sign-off regardless of confidence, mapping this repo's Trust Controls
in [`docs/business-model.md`](docs/business-model.md)):
`:flag-compliance-concern` (always, never auto-commit-eligible) and
`:coordinate-supply-order` above the permit's registered
supply-order cost threshold.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a
**robot performs the physical/administrative domain work**. Here a
logistics-coordination robot performs catch/harvest record entry
(post-hoc metadata only, never a kill/harvest-timing decision),
equipment-maintenance scheduling and non-weapon-supplies procurement
paperwork under an actor that proposes actions and an independent
**Hunting Trapping Governor** that gates them. The governor never
dispatches hardware itself and never fires a weapon, sets or springs a
trap, or makes a kill/harvest-timing decision —
`:flag-compliance-concern` actions always require human hunter/trapper
sign-off, and no op resembling those execution actions exists in the
allowlist at all.

## Core Contract

```text
hunter/trapper request (harvest log / maintenance / procurement / concern)
        |
        v
Hunting Trapping Advisor -> Hunting Trapping Governor -> log/schedule/order, or human sign-off
        |
        v
robot actions (gated, documentation/logistics only) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses,
suppress an operating record, or exercise any weapon-firing,
trap-setting/springing or kill/harvest-timing-decision authority —
that authority does not exist in this actor's vocabulary.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `6224`). Required capabilities:

- :robotics
- :identity
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
