#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
curl -fsS http://localhost:8080/actuator/health | python3 -m json.tool
RESP=$(curl -fsS -X POST http://localhost:8080/api/cases \
 -H 'Content-Type: application/json' \
 -d '{"reference":"DEMO-'"$(date +%s)"'","amount":15000,"currency":"USD","newBeneficiary":true,"sanctionsHit":false,"accountMismatch":true,"narrative":"Synthetic beneficiary mismatch"}')
printf '%s\n' "$RESP" | python3 -m json.tool
printf '\nOpen the dashboard at http://localhost:8080\n'
