# Forward Deployed Engineer JD coverage

| JD requirement | Actual implementation | Production gap |
|---|---|---|
| Java/AWS/Bedrock | Java 25, Spring Boot, Bedrock Converse and Titan embedding adapters | AWS access and live validation |
| Banking domain | Payment exception workflow and synthetic core REST sandbox | Real bank integration and domain signoff |
| Multi-agent patterns | Branching, bounded workflow states and recorded trace | Dynamic model tool planning, long-running jobs |
| RAG/context | pgvector 1024D + versioned illustrative policy corpus | Enterprise corpora, search benchmarks |
| Human oversight | Authenticated review; sanctions fail-closed | Enterprise reviewer groups and separation of duties |
| Audit/compliance | Append-only PostgreSQL table and SHA-256 chain | External immutable/WORM audit, SOC and privacy controls |
| Intelligent CI/CD | Maven tests, golden evaluation, Trivy and Dependabot | Signed artifacts, manual gates, IaC scanning |
| MLOps | Offline eval fixture, optional chargeable live model gate and metric counters | Production evaluation automation, drift monitoring, cost governance |
| Infrastructure | Docker + Terraform for ECS, RDS, ECR, ALB, Secrets Manager, CloudWatch | Apply with real network/certs/SSO and harden for HA |
| Frontend | Local analyst dashboard | Production UI identity, CSRF / UX hardening |
| Stakeholder leadership | Problem brief, acceptance criteria, architecture, demo script | Real stakeholder feedback and measured business KPIs |

The local demo is not a certified production banking platform. Use artifacts as genuine code evidence without exaggeration.
