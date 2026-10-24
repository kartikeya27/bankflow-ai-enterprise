# REST API reference

- `GET /actuator/health` — health (public).
- `POST /api/cases` — JSON `CaseInput`: `reference`, `amount`, `currency=USD`, `newBeneficiary`, `sanctionsHit`, `accountMismatch`, `narrative`. Creates a case. Returns 201.
- `GET /api/cases` — most recent 100 persisted case records.
- `GET /api/cases/{uuid}` — one case.
- `POST /api/cases/{uuid}/decision` — JSON `{ "decision":"APPROVE|REJECT", "note":"reason" }`, authenticated reviewer.
- `GET /api/cases/{uuid}/audit` — reviewer-only audit chain and verification.
- `GET /api/policies` — five illustrative policies.
- `GET /api/metrics` — DB-backed counters.
- `GET /actuator/prometheus` — local monitoring only; deliberately denied externally in prod.

Local UI is at `/`. No API can initiate a banking transaction. HTTP 409 for sanctions approval or reused reference with changed payload; HTTP 503 when required core validation is unavailable.

See complete curl commands in `docs/SETUP_EVERY_STEP.md`.
