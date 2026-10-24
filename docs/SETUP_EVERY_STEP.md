# BankFlow AI Enterprise v2 — Full macOS build, verification, and deployment runbook

**This guide is intentionally explicit. Commands starting `cd ~/projects/bankflow-ai-enterprise` run on your Mac, not in Docker or AWS CloudShell.**
The example is fictitious and never moves money. Java 25 and Maven compile locally; PostgreSQL with pgvector and the mock bank run in Docker. Real AWS, OIDC and bank integrations must be provisioned separately.

## A. Environment and first installation

**1. Open Terminal.** Check OS and chip:

```bash
sw_vers
uname -m
pwd
```

**2. Check Homebrew, Git, Docker, Maven and Java:**

```bash
brew --version
git --version
docker --version
mvn --version
java -version
```

If Homebrew is missing, install it using the current instructions at https://brew.sh. Docker Desktop is a separate download from https://docs.docker.com/desktop/setup/install/mac-install/ and must be started.

**3. Install dependencies that are missing:**

```bash
brew install openjdk@25 maven git gh jq awscli terraform
```

**4. Set Java 25 for the current shell:**

```bash
export JAVA_HOME="$(brew --prefix openjdk@25)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
mvn -version
```

Java must report major version 25.

**5. Persist the Java setup for future zsh terminals, once only:**

```bash
cat >> ~/.zshrc <<'EOF'
export JAVA_HOME="$(brew --prefix openjdk@25)/libexec/openjdk.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
EOF
source ~/.zshrc
```

**6. Make a workspace:**

```bash
mkdir -p ~/projects
cd ~/projects
```

**7. Create the repository using one of these methods:**

From the readable project generator (recommended, no ZIP):

```bash
bash ~/Downloads/CREATE_BANKFLOW_ENTERPRISE_ALL_FILES.sh
```

Or extract the ZIP if preferred:

```bash
unzip ~/Downloads/bankflow-ai-enterprise-v2.zip -d ~/projects
```

**8. Enter the directory:**

```bash
cd ~/projects/bankflow-ai-enterprise
pwd
ls -la
find . -type f | sort
```

If the creator refuses because the directory already exists, back up your current work or choose ZIP extraction in a different directory. Do not delete your previous project just to proceed.

**9. Run the dependency-free structure/source checks:**

```bash
python3 scripts/verify-project.py
```

Expected: `PASS static required file, XML, JSON, Python and shell validation`.

**10. Inspect the configuration.** The distributed sample is safe only for a localhost demo:

```bash
cat .env.example
cat src/main/resources/application.yml
cat compose.yaml
```

**11. Create an untracked `.env` for Compose:**

```bash
cp .env.example .env
chmod 600 .env
```

Do not commit `.env`. For a shared workstation, replace its demonstration credentials with local random values before use. The default credentials in the application are **not production credentials**.

**12. Start Docker Desktop** and verify daemon connectivity:

```bash
docker info >/dev/null && echo 'Docker running'
docker compose version
```

## B. Start PostgreSQL, vector search and mock banking service

**13. Start the persistent database and the separate HTTP mock-bank:**

```bash
cd ~/projects/bankflow-ai-enterprise
docker compose up -d db mock-core
```

**14. Check the containers:**

```bash
docker compose ps
docker compose logs --tail=40 db
docker compose logs --tail=20 mock-core
```

**15. Verify PostgreSQL is healthy:**

```bash
docker compose exec db pg_isready -U bankflow -d bankflow
```

Expected: `accepting connections`.

**16. Verify the mock-bank is alive:**

```bash
curl -fsS http://localhost:8090/health | jq .
```

Expected: `{"status":"UP"}`. It never moves funds. You can inspect `mock-core/server.py`.

**17. Ensure Docker database credentials match the Java defaults.** Unless you changed `.env`, use:

```bash
export DB_URL='jdbc:postgresql://localhost:5432/bankflow'
export DB_USER='bankflow'
export DB_PASSWORD='local-db-only'
export AI_MODE=mock
export EMBEDDING_MODE=mock
export CORE_MODE=http
export CORE_URL=http://localhost:8090
export CORE_API_KEY=demo-core-key
```

When you change DB_PASSWORD after a database volume was first initialized, Compose environment alone does NOT change the already-created PostgreSQL password. Keep the original or intentionally rotate the DB role password. **Do not delete the DB volume unless you want to lose cases.**

## C. Build, run and validate the full local application

**18. Compile:**

```bash
mvn -B -ntp clean compile
```

Expected: `BUILD SUCCESS`. If not, capture the FIRST Maven error; don't edit random files.

**19. Execute automated integration tests and the deterministic five-case evaluation harness:**

```bash
mvn -B -ntp clean test
```

Expected: `BUILD SUCCESS`, with tests for risk, audit, sanctions holds, idempotency, retrieval and HTTP security.

**20. Package the application:**

```bash
mvn -B -ntp clean package
ls -lh target/bankflow-ai-2.0.0.jar
```

**21. Run the server in mock LLM mode with a REAL HTTP banking sandbox:**

```bash
mvn spring-boot:run
```

Keep this terminal open. This mode has **no Bedrock cost**. It connects to PostgreSQL and to `http://localhost:8090`.

**22. Open a SECOND Terminal tab** for HTTP/API checks. Verify:

```bash
curl -fsS http://localhost:8080/actuator/health | jq .
```

Expected `{"status":"UP"}`.

**23. Open the web UI** in Chrome/Safari:

```bash
open http://localhost:8080
```

**24. Create a high-risk synthetic case:**

```bash
REF="DEMO-$(date +%s)"
curl -fsS -X POST http://localhost:8080/api/cases \
 -H 'Content-Type: application/json' \
 -d '{"reference":"'"$REF"'","amount":15000,"currency":"USD","newBeneficiary":true,"sanctionsHit":false,"accountMismatch":true,"narrative":"Synthetic beneficiary mismatch"}' | tee /tmp/bankflow-result.json | jq .
```

Expected: `riskLevel=HIGH`, `status=PENDING_REVIEW`, `llmProvider=MOCK` (or its exact mock provider string), and trace stages including the mock-bank integration and vector policy retrieval.

**25. Save its UUID:**

```bash
CASE_ID=$(jq -r .id /tmp/bankflow-result.json)
echo "$CASE_ID"
```

**26. Verify the record is stored in PostgreSQL:**

```bash
docker compose exec db psql -U bankflow -d bankflow -c 'SELECT reference,status,created_at FROM bank_cases ORDER BY created_at DESC LIMIT 5;'
```

**27. Verify the pgvector extension and indexed policy corpus:**

```bash
docker compose exec db psql -U bankflow -d bankflow -c "SELECT extname FROM pg_extension WHERE extname='vector';"
docker compose exec db psql -U bankflow -d bankflow -c 'SELECT id,version,embedding_provider,vector_dims(embedding) AS dimensions FROM policy_documents ORDER BY id;'
```

Expected: `vector` extension and five policies with `dimensions=1024`.

**28. Check human approval is protected:**

```bash
curl -i -sS -X POST "http://localhost:8080/api/cases/$CASE_ID/decision" \
 -H 'Content-Type: application/json' \
 -d '{"decision":"APPROVE","note":"Synthetic verified reviewer"}'
```

Expected HTTP `401`.

**29. Approve the non-sanctions case with local reviewer credentials:**

```bash
curl -fsS -u reviewer:local-demo-only -X POST "http://localhost:8080/api/cases/$CASE_ID/decision" \
 -H 'Content-Type: application/json' \
 -d '{"decision":"APPROVE","note":"Synthetic case reviewed; no funds moved"}' | jq '{id,status,reviewedBy}'
```

Expected status `REVIEW_APPROVED`. This does **not** execute a payment.

**30. Check hash-chained audit entries:**

```bash
curl -fsS -u reviewer:local-demo-only "http://localhost:8080/api/cases/$CASE_ID/audit" | jq .
```

Expected `verified: true`, events for `CASE_CREATED` and `REVIEW_APPROVE`.

**31. Run a sanctions test:**

```bash
ID=$(curl -fsS -X POST http://localhost:8080/api/cases -H 'Content-Type: application/json' \
 -d '{"reference":"SANCTIONS-'"$(date +%s)"'","amount":300,"currency":"USD","newBeneficiary":false,"sanctionsHit":true,"accountMismatch":false,"narrative":"Synthetic possible hit"}' | jq -r .id)
echo "$ID"
curl -sS -o /tmp/sanctions-approval.json -w 'HTTP %{http_code}\n' \
 -u reviewer:local-demo-only -X POST "http://localhost:8080/api/cases/$ID/decision" \
 -H 'Content-Type: application/json' \
 -d '{"decision":"APPROVE","note":"Attempting forbidden approval"}'
```

Expected HTTP `409`. A sanctions flag routes to Compliance Supervisor and cannot be approved.

**32. Prove that cases survive application restarts:**

```bash
bash scripts/assert-persistence.sh
```

Copy the resulting UUID (saved in `/tmp/bankflow-last-case-id`). Stop only the Java process with Ctrl+C, restart with `mvn spring-boot:run`, then in the second terminal:

```bash
PERSIST_ID=$(cat /tmp/bankflow-last-case-id)
curl -fsS "http://localhost:8080/api/cases/$PERSIST_ID" | jq '{id,status,riskLevel}'
```

Expected: the same UUID and case status. Don't run `docker compose down -v`.

**33. Check metrics:**

```bash
curl -fsS http://localhost:8080/actuator/prometheus | grep bankflow_
curl -fsS http://localhost:8080/api/metrics | jq .
```

## D. Optional: call Amazon Bedrock for real

**34. Confirm you have an AWS account, payment permissions and budget alerts.** Bedrock can incur charges. Never use real PII or transaction data.

**35. Configure AWS IAM Identity Center if available:**

```bash
aws configure sso --profile bankflow-demo
aws sso login --profile bankflow-demo
aws sts get-caller-identity --profile bankflow-demo
```

Or use organization-approved temporary AWS credentials. Avoid plain access keys committed to files.

**36. Enable model access in the AWS Region.** Verify access to `amazon.nova-lite-v1:0` (or an approved compatible Converse model) and Titan Text Embeddings V2 `amazon.titan-embed-text-v2:0`, including IAM `bedrock:InvokeModel` permissions. Bedrock models and region availability change; consult AWS docs.

**37. Restart Java in live inference + vector embedding mode:**

```bash
# Stop Spring Boot with Ctrl+C before changing these variables.
export AWS_PROFILE=bankflow-demo
export AWS_REGION=us-east-1
export AI_MODE=bedrock
export EMBEDDING_MODE=bedrock
export BEDROCK_MODEL_ID=amazon.nova-lite-v1:0
export EMBEDDING_MODEL_ID=amazon.titan-embed-text-v2:0
mvn spring-boot:run
```

The five policy documents are re-embedded on startup to avoid mixing local hash vectors with Titan vectors. RAG and explanation inference generate chargeable model requests.

**38. Create a new synthetic case:**

```bash
curl -fsS -X POST http://localhost:8080/api/cases -H 'Content-Type: application/json' \
 -d '{"reference":"AWS-'"$(date +%s)"'","amount":12000,"currency":"USD","newBeneficiary":true,"sanctionsHit":false,"accountMismatch":false,"narrative":"Synthetic account review"}' | jq '{riskLevel,status,llmProvider,policies,explanation}'
```

Expected `llmProvider` starting `AWS_BEDROCK:` if inference was successful. `SAFE_FALLBACK` indicates inference failure or rejected citation, not successful Bedrock use.

**39. Run the optional, chargeable live model-evaluation harness:**

```bash
python3 scripts/evaluate-live.py
cat reports/model-evaluation.json
```

Expected six scenarios and `pass_rate`. A nonzero exit status indicates a failed evaluation. This evaluates policy citation, deterministic safety, provider use, and some injection strings, **not holistic factual correctness or model risk validation**.

**40. Go back to free local mode** by stopping Java and running:

```bash
export AI_MODE=mock
export EMBEDDING_MODE=mock
mvn spring-boot:run
```

## E. Run everything in containers (alternative to local Maven runtime)

**41. Stop local Maven application with Ctrl+C** to free port 8080.

**42. Start full Docker Compose stack:**

```bash
cd ~/projects/bankflow-ai-enterprise
docker compose --profile full up --build -d
```

This launches app + PostgreSQL/pgvector + mock-core in safe mock AI mode. `docker compose ps` shows health/status. If Maven or Docker cannot download dependencies/images, check internet and proxy settings.

**43. Inspect container logs:**

```bash
docker compose logs --tail=80 app
curl -fsS http://localhost:8080/actuator/health | jq .
```

**44. Shut down without deleting data:**

```bash
docker compose --profile full down
```

**45. Optionally monitor Prometheus** when the application is running locally (not with production auth):

```bash
docker compose --profile monitoring up -d prometheus
open http://localhost:9090
```

Prometheus scrapes `host.docker.internal:8080`. On some Linux environments this host alias requires extra Compose settings; the configuration targets Docker Desktop on macOS.

## F. Create the GitHub repository without losing existing work

**46. Check for a Git repo:**

```bash
cd ~/projects/bankflow-ai-enterprise
git status
```

If output says `not a git repository`, initialize it:

```bash
git init -b main
```

**47. Inspect secrets and staged files:**

```bash
git add .
git status --short
git check-ignore .env target/ || true
```

Never commit `.env`, `*.tfvars`, `terraform.tfstate` or real customer records.

**48. Commit:**

```bash
git commit -m "feat: BankFlow enterprise AI banking platform v2"
```

**49. Sign in through GitHub CLI:**

```bash
gh auth login
gh auth status
```

**50. Create a new public GitHub repo:**

```bash
gh repo create bankflow-ai-enterprise --public --source=. --remote=origin --push
```

If you already created this repo, do not force-push over it. Use `git remote -v`, `git fetch origin`, and reconcile the histories.

**51. Inspect the Actions tab** for a passed workflow. GitHub Actions runs Maven tests and the offline golden-case evaluation against an ephemeral Postgres/pgvector service, then scans the source with Trivy.

## G. Optional Terraform AWS deployment — requires real external prerequisites and incurs costs

**52. Important architecture:** `infra/terraform` creates ECR, ECS/Fargate, RDS, private security groups, an HTTPS ALB, CloudWatch log group, IAM task roles, Secrets Manager password and Route53 DNS. It relies on an **existing** VPC, at least two appropriate public and private subnets, NAT or VPC endpoints for all dependencies, ACM TLS cert, Route53 public zone, OIDC tenant, and approved HTTPS mock/sandbox bank URL. No bank payment execution is ever implemented. This has not been deployed in this container.

**53. Create a real AWS budget before applying.** RDS, NAT, ALB, Fargate, DNS and Bedrock create ongoing expenses. Avoid `terraform apply` unless you approve these charges.

**54. Configure terraform input:**

```bash
cd ~/projects/bankflow-ai-enterprise/infra/terraform
cp terraform.tfvars.example terraform.tfvars
```

Edit `terraform.tfvars` with **actual** subnet IDs, VPC ID, ACM certificate, Route53 hostname and zone ID, OIDC issuer URL, and approved sandbox HTTPS URL. `terraform.tfvars` is ignored by Git. Create the approved mock-core key as a Secrets Manager secret and set `core_sandbox_key_secret_arn`.

**55. Authenticate AWS and validate syntax:**

```bash
export AWS_PROFILE=bankflow-demo
terraform init
terraform fmt -recursive
terraform validate
terraform plan -out=tfplan
```

Review the complete plan, resources, access policies and estimated charges.

**56. Create only ECR first** so the image can be published before ECS starts:

```bash
terraform apply -target=aws_ecr_repository.app
```

Targeted applies are used here only to bootstrap ECR; don't use them for routine changes.

**57. Build and push the image:**

```bash
cd ~/projects/bankflow-ai-enterprise
AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGION=us-east-1
REGISTRY="$AWS_ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"
docker build -t bankflow-ai-enterprise:v2 .
docker tag bankflow-ai-enterprise:v2 "$REGISTRY/bankflow-ai:v2"
docker push "$REGISTRY/bankflow-ai:v2"
```

**58. Apply full infrastructure** from the terraform directory:

```bash
cd infra/terraform
terraform plan -out=tfplan
terraform apply tfplan
terraform output
```

Check ECS task health, logs and HTTPS hostname. The local demo reviewer password is **not used** in the production JWT profile. Production requires JWT scopes and live Bedrock inference, and refuses to start with mock core or mock AI.

**59. Verify cloud monitoring and identity:** Test production health from HTTPS. Configure your actual OIDC clients and issue scoped JWT tokens, then test read/write/review permissions. Investigate ECS failures through CloudWatch logs. If DB `CREATE EXTENSION vector` is refused, your RDS DB role must be authorized for that extension and migration. Do not bypass privileges globally.

**60. Destroy only after careful review:**

```bash
terraform plan -destroy
# Do not run destroy casually. RDS deletion protection and final snapshots are intentional.
```

Terraform provisioned services are examples and are **not security/compliance-certified**. A real bank should require IAM/network reviews, availability/restore testing, key rotations, private egress, independent approvals and model governance signoff.

## H. Troubleshooting and practical file reading

- `mvn: command not found`: `brew install maven`.
- Java not 21: repeat Step 4, then use `which java` and `echo "$JAVA_HOME"`.
- `Connection refused 5432`: start Docker Desktop and `docker compose up -d db`.
- `type vector does not exist`: verify you used the pgvector image and that Flyway ran.
- `Port 8080 in use`: `lsof -nP -iTCP:8080 -sTCP:LISTEN`; stop old Spring Boot or set `PORT=8081`.
- HTTP 503 for `/api/cases`: required `CORE_MODE=http` mock-bank may be stopped, unavailable or returned an error. Check `docker compose logs mock-core` and `curl localhost:8090/health`.
- HTTP 401 on decision: give local reviewer Basic credentials; production JWT scopes differ.
- HTTP 409 for sanctions approval: expected. Do not change the control to pass this test.
- Re-running the same reference with a different payload: HTTP 409 is intentional idempotency protection.
- `SAFE_FALLBACK`: inspect AWS region/model permissions, model access, citation grounding; deterministic risk still applies.
- Docker DB password changed in `.env` after first boot: existing volume's role is unchanged. Rotate credentials intentionally, don't delete data automatically.
- `terraform plan` fails: fill in **all** required `terraform.tfvars` with valid AWS values, role permissions and existing network IDs.
- Git `main -> main (fetch first)`: never blindly force push; reconcile remote changes after `git fetch origin`.

## I. Learning sequence by concrete files

Read in this order: `src/main/resources/db/migration/V1__schema.sql`, `model/CaseInput.java`, `service/CoreBankGateway.java`, `service/HttpCoreGateway.java`, `service/WorkflowEngine.java`, `service/PolicyKnowledgeBase.java`, `service/EmbeddingService.java`, `service/BedrockEmbeddingService.java`, `service/CaseService.java`, `service/AuditService.java`, `config/SecurityConfiguration.java`, `web/CaseController.java`, `src/test/java/...`, `.github/workflows/ci.yml`, and `infra/terraform/*.tf`.

**Source inventory:** all files are included in the companion `BANKFLOW_ENTERPRISE_ALL_SOURCE.md` with precise paths and complete unabridged contents. The repository is also available as a ZIP or as the readable creator script.
