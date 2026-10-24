# Security and governance

- Local demo has an authenticated reviewer role via HTTP Basic and may be run only on localhost.
- Production profile requires a trusted OIDC issuer and JWT scopes (`bankflow.read`, `bankflow.write`, `bankflow.review`).
- Human reviewers cannot approve sanctions cases. No funds transfer endpoint exists.
- PostgreSQL case and audit writes are committed in one transaction. Audit event rows have
  UPDATE and DELETE triggers disabled, and a per-case SHA-256 chain is checked by `/api/cases/{id}/audit`.
- Changes by superusers, backups and external storage are outside this guarantee.
- AI outputs cannot modify risk or case state; unknown `P-###` citations cause fallback.
- AI-generated explanation still needs content-quality review. Prompts are not enough to prevent injection.
- Policy text and banking references are purely synthetic. Never use production PII or actual bank secrets.
- Production must use private VPC networking, encrypted stores, strict IAM, managed credentials,
  immutable log export, change controls, penetration testing, and legal review before use.
- Trivy and offline evaluation in CI are helpful gates but not regulatory certifications.

See `docs/THREAT_MODEL.md` and `docs/ACCEPTANCE_CRITERIA.md`.
