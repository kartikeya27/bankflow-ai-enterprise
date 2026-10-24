# BankFlow AI Enterprise — Forward Deployed Engineer Portfolio

## Technology Stack

Java 21 · Spring Boot · AWS Bedrock · PostgreSQL/pgvector · Branching AI workflow · Banking sandbox integration · Audit · CI/evaluations · Terraform AWS ECS/RDS

This is a local and cloud-deployable **engineering demonstration** using fictitious banking data. It is not a bank-certified system, does not move funds, and has not been deployed or penetration-tested in this environment.

## Getting Started

1. Read [docs/SETUP_EVERY_STEP.md](docs/SETUP_EVERY_STEP.md) for detailed installation steps, commands, and validation checks.
2. Install Java 21, Maven, Docker Desktop, Python 3, and Git.
3. Start PostgreSQL and the mock banking service using `docker compose up -d db mock-core`.
4. Run `mvn clean test` to execute the tests (integration tests require PostgreSQL).
5. Start Spring Boot using `mvn spring-boot:run`.
6. Open [http://localhost:8080](http://localhost:8080).

## Architectural Features

- **Persisted case and review state:** PostgreSQL transaction commit and unique-case-reference idempotency.
- **Vector retrieval:** pgvector 1024-dimensional embeddings; local token hash offline, AWS Titan V2 opt-in. Enforced policies are always included.
- **Bounded workflow:** Maximum-step agent state machine with compliance branching, deterministic risk evaluation, and human approval.
- **Grounded explanations:** Mock no-cost generator or AWS Bedrock Converse; invalid policy citations trigger fallback.
- **Banking sandbox adapter:** Java REST client with timeout and retry, backed by the `mock-core/server.py` HTTP service.
- **Transactional audit:** Hash-chained append-only database table with a case-specific integrity verification endpoint.
- **Environment-specific identity:** Localhost Basic authentication for reviewers; JWT scopes/OIDC in the separate production Spring profile.
- **Evaluation and quality:** Five deterministic golden evaluation fixtures and six optional live Bedrock quality checks.
- **Observability and deployment:** Micrometer Prometheus endpoint, Docker, GitHub Actions with Trivy, and Terraform definitions for ECS, RDS, ALB, ECR, and CloudWatch.

## Safety and Scope

The AWS blueprint requires a real AWS account, VPC/subnets with egress, Route 53, ACM TLS, an OIDC issuer, Secrets Manager sandbox credentials, Bedrock model permissions, RDS pgvector support, and production security review.

AWS resources may incur charges.

Hash-chaining does not protect against privileged database administrators or all forms of tampering. The vector corpus contains five synthetic policies, and the local hash embeddings are not production retrieval quality.

The workflow is bounded and branching, not open-ended autonomous LLM tool planning. CI does not constitute regulatory certification.

See [Threat Model](docs/THREAT_MODEL.md) and [Acceptance Criteria](docs/ACCEPTANCE_CRITERIA.md) for further documentation.

## File-by-File Source

The project includes the Java source code, application resources, tests, infrastructure definitions, and scripts.

The companion document `BANKFLOW_ENTERPRISE_ALL_SOURCE.md` provides the complete source listing with exact file paths.

The readable script `CREATE_BANKFLOW_ENTERPRISE_ALL_FILES.sh` can recreate the project structure.

## Author

Kartikeya Bhatt
