# Implementation walkthrough

1. Request DTO validation: `model/CaseInput.java`.
2. REST layer and review: `web/CaseController.java`.
3. Idempotency, transactional persistence and DB metrics: `service/CaseService.java`.
4. Bounded branching agent state machine: `service/WorkflowEngine.java`.
5. Sandbox bank integration: `service/CoreBankGateway.java`, `service/HttpCoreGateway.java`, `mock-core/server.py`.
6. Vector policy indexing and retrieval: `service/PolicyKnowledgeBase.java`, `service/EmbeddingService.java` and both concrete embedding adapters.
7. Bedrock explanation: `service/BedrockTextGenerator.java`.
8. Security: `config/SecurityConfiguration.java`, prod-only `config/ProductionGuard.java`.
9. Audit: `service/AuditService.java`, Flyway migration `db/migration/V1__schema.sql`.
10. Validation, eval and security: `src/test`, `scripts/evaluate-live.py`, `.github/workflows/ci.yml`.
11. Infrastructure and ops: `compose.yaml`, `Dockerfile`, `observability/prometheus.yml`, `infra/terraform`.

For concrete commands and evidence collection, follow `docs/SETUP_EVERY_STEP.md`.
