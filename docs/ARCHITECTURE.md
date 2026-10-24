# BankFlow AI Enterprise v2 architecture

```text
Browser (local) --REST--> Spring Boot Controllers
                         |
                         +-- Security: local Basic / production OIDC JWT
                         +-- Bounded WorkflowEngine (max 12 transitions)
                         |     INTAKE -> CORE -> CLASSIFY -> RETRIEVE -> RISK
                         |                                            |
                         |                         sanctions? -> COMPLIANCE
                         |                                            |
                         |                                EXPLAIN -> REVIEW
                         +-- CoreBankGateway mock / HTTP mock-bank sandbox
                         +-- Bedrock Converse or deterministic explanation
                         +-- Bedrock Titan V2 embeddings or local 1024-D hash vectors
                         +-- PostgreSQL + pgvector
                               bank_cases (persistent case JSON)
                               policy_documents (version + 1024D embeddings)
                               audit_events (append-only hash chain)
```

Technical choices: deterministic hard controls are authority; the LLM only drafts explanations;
trace and policies are retained with each case; transaction commit includes case and audit.
Data migrations run via Flyway. All banking cases use fictitious references and amounts.

**Not claimed:** open-ended autonomous tool-planning, certified bank integration, tamper-proof audit
against DB superusers, full PII lifecycle, comprehensive SLO controls, production security approval.
The Terraform deployment requires existing VPC subnets with egress, IAM permissions,
a TLS certificate, DNS, an OIDC issuer, authorized Bedrock models, and an approved banking sandbox.
