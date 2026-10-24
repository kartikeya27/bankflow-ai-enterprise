#!/usr/bin/env bash
set -euo pipefail
BASE="${BASE_URL:-http://localhost:8080}"
printf 'Health: '; curl -fsS "$BASE/actuator/health"; echo
printf '\nNew case:\n'
curl -fsS -X POST "$BASE/api/cases" -H 'Content-Type: application/json' \
  -d '{"reference":"DEMO-001","amount":12500,"currency":"USD","newBeneficiary":true,"sanctionsHit":false,"accountMismatch":false,"narrative":"Synthetic incoming payment exception"}'
printf '\n\nMetrics:\n'; curl -fsS "$BASE/api/metrics"; echo
