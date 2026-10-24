# BankFlow AI Enterprise v2 — Forward Deployed Engineer portfolio

**Java 25 · Spring Boot · AWS Bedrock · PostgreSQL/pgvector · Branching AI workflow · Banking sandbox integration · Audit · CI/evaluations · Terraform AWS ECS/RDS**

This is a local and cloud-deployable **engineering demonstration** using fictitious banking data. **It is not a bank-certified system, does not move funds, and has not been deployed or penetration-tested in this environment.**

## Begin here — no missing files

1. Read **[`docs/SETUP_EVERY_STEP.md`](docs/SETUP_EVERY_STEP.md)** (60 detailed steps, commands and checks).
2. Install Java 25, Maven, Docker Desktop, Python 3 and Git.
3. `docker compose up -d db mock-core`
4. `mvn clean test` (integration tests need database)
5. `mvn spring-boot:run`
6. Open `http://localhost:8080`.

### Architectural features

- **Persisted case and review state:** PostgreSQL transaction commit and unique-case-reference idempotency.
- **Vector retrieval:** pgvector 1024-D embeddings; local token hash offline, AWS Titan v2 opt-in. Enforced policies always included.
- **Bounded workflow:** max-step agent state machine with compliance branch, deterministic risk decision and human approval.
- **Grounded explanations:** mock no-cost generator or AWS Bedrock Converse; invalid policy citations trigger fallback.
- **Separate banking sandbox adapter:** Java REST client with timeout and retry, backed by `mock-core/server.py` HTTP service.
- **Transactional audit:** hash-chained append-only table, case-specific integrity-check endpoint.
- **Environment-specific identity:** localhost Basic reviewer; JWT scopes/OIDC in the separate `prod` Spring profile.
- **Evaluation/quality:** five deterministic golden evaluation fixtures and six optional live Bedrock proxy quality checks.
- **Observability/deployment:** Micrometer Prometheus endpoint; Docker, GitHub Actions with Trivy, Terraform ECS/RDS/ALB/ECR/CloudWatch.

### Safety and scope

The AWS blueprint needs a real AWS account, VPC/subnets with egress, Route53 and ACM TLS, OIDC issuer, Secrets Manager sandbox credential, Bedrock model permissions, RDS pgvector support and production review. AWS resources create charges. Hash-chaining does not protect against powerful DB administrators or all forms of tampering. The vector corpus has only five synthetic policies and the local hash embeddings are not production retrieval quality. The workflow is bounded and branching, **not open-ended autonomous LLM tool planning**. CI does not constitute regulatory certification. See [`docs/THREAT_MODEL.md`](docs/THREAT_MODEL.md) and [`docs/ACCEPTANCE_CRITERIA.md`](docs/ACCEPTANCE_CRITERIA.md).

### File-by-file source

The bundle ships **every source file**. See the companion Markdown `BANKFLOW_ENTERPRISE_ALL_SOURCE.md` for the complete contents with exact paths. The project is reproducible using the readable `CREATE_BANKFLOW_ENTERPRISE_ALL_FILES.sh`.
