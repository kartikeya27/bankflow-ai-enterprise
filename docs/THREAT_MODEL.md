# Threat model and trust boundaries

- Browser and applicant narrative are **untrusted** input. The system bounds length and uses typed DTOs.
- No `POST /payments/execute` functionality exists; decisions only update **case status**.
- Backend controls, not LLM text, decide risk and sanctions escalation.
- Production is a JWT resource server. Supply a trusted issuer and verified bankflow scopes.
- Local Basic auth credentials must NEVER be internet accessible.
- Database access is private in Terraform. The task role can invoke only configured Bedrock models.
- Audit rows cannot be mutated with ordinary UPDATE/DELETE; SHA256 chain supports integrity checks,
  but database owners, backups, privileged administrators and log storage are still trust boundaries.
- Mock `bank-core` uses a demonstration header key and is NOT a genuine bank credential strategy.
- Model outputs can contain unsupported advice; policy-ID references are checked, but semantic truthfulness
  still requires human review and deeper model-specific evaluation.
- `CORE_MODE=http` uses a read-only sandbox validation contract, never a money-moving endpoint.
- Secrets belong in Secret Manager or another approved secrets provider; no real PII in demos.
- RDS in provided Terraform defaults to single-AZ for portfolio budget; enterprise requires multi-AZ,
  WAF/rate limiting, backup/restore tests, encryption policy checks, network egress controls, full logging,
  vulnerability remediation, IAM security reviews and organizational approval.
