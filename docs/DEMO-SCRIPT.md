# Seven-minute FDE interview demonstration

1. **1 minute — discover problem:** bank operations teams manually triage exception cases. Constraints include data handling, compliance, and traceable decisions.
2. **1 minute — show architecture:** Java API, core banking sandbox adapter, workflow orchestrator, PostgreSQL/pgvector, Bedrock, review, audit, IAM.
3. **1 minute — create case:** $15,000 payment with mismatch. Show HIGH risk, mandatory P-001/P-004 and vector evidence, explanation and workflow trace.
4. **1 minute — safety:** run sanctions case, attempt reviewer approval, receive 409; no payment execution endpoint.
5. **1 minute — reliability:** show mock-bank timeout blocking case rather than bypassing external verification. Show persistent DB row after restart.
6. **1 minute — engineering:** run `mvn test`, discuss golden evaluations, hash-chained audit, GitHub CI/Trivy, metrics.
7. **1 minute — cloud rollout:** discuss AWS Bedrock and Terraform blueprint; show production OIDC JWT scopes and what still requires a bank and governance sign-off.

**Do not claim:** this project was deployed to a bank, approved by compliance, has real customer data or processes payments.
