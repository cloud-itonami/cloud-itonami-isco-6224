# Security Policy

This project handles hunting/trapping logistics-coordination workflows.
Treat vulnerabilities as potentially high impact even when the demo data is
synthetic — this occupation carries real weapons-handling and animal-welfare
stakes in the physical world, and this actor's entire design premise is that
none of that authority is reachable through this codebase.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real operator, permit or harvest-record data exposure
- authorization bypass
- Hunting Trapping Governor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- unsafe robot action dispatch
- **any path by which a proposal, op or rationale could be interpreted as
  firing a weapon, setting or springing a trap, or making a
  kill/harvest-timing decision** — report this as a critical-severity
  design flaw even if no exploit is demonstrated, since the entire actor's
  safety case rests on that surface being structurally empty

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on operator/permit/harvest-record data, policy enforcement or audit
  logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real operator/permit/harvest-record data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
