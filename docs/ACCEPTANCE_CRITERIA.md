# Definition of done for the extended demo

| Capability | Acceptance evidence | Remaining enterprise dependency |
|---|---|---|
| Persistent case state | Create case, restart app, GET same ID | Multi-AZ backup/restore tests |
| Transactional decision/audit | `CaseServiceTest`, read `audit_events`, verify chain | External immutable audit storage |
| Branching workflow | Sanctions route includes `Compliance Supervisor` | AI tool-call planning and autonomous retry strategy |
| Retrieval augmented response | 1024-D pgvector policy documents, mandatory IDs always included | Larger domain corpus, relevance evaluation |
| Bedrock inference | AWS `converse` request when enabled | Model authorization, spend budget, live quality gate |
| Bedrock embeddings | Titan V2 vector(1024) on opt-in | RAG domain benchmark and reindex scheduling |
| Core integration | HTTP sandbox with idempotency key and timeout | Real banking system test environment and auth |
| Authentication | Local Basic; OIDC/JWT production resource server | Identity provider configuration and verified scopes |
| Evaluations | Five golden deterministic evaluations in CI | Ground-truth LLM eval harness with cost/latency targets |
| CI and security | GitHub Actions tests and Trivy filesystem scan | Signed releases, review approvals, SBOM, policy-as-code |
| Observability | Micrometer /actuator/prometheus, CloudWatch logs in Terraform | Tracing, paging, SLOs, redaction assurance |
| Deployment | Terraform ECR, ECS, ALB HTTPS, RDS, IAM, Secrets Manager | Real AWS account, subnet/NAT, ACM, approved SSO and bank sandbox |

Do **not** claim production certification or guaranteed security/compliance.
