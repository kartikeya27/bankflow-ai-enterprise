#!/usr/bin/env bash
set -euo pipefail
reference="PERSIST-$(date +%s)"
res=$(curl -fsS -X POST http://localhost:8080/api/cases -H 'Content-Type: application/json' -d \
  '{"reference":"'"$reference"'","amount":12000,"currency":"USD","newBeneficiary":true,"sanctionsHit":false,"accountMismatch":false,"narrative":"Synthetic persistence check"}')
id=$(printf '%s' "$res" | python3 -c 'import json,sys;print(json.load(sys.stdin)["id"])')
echo "New case: $id"
echo "$id" > /tmp/bankflow-last-case-id
printf '%s\n' 'Restart the Java app, then run:'
printf '%s\n' '  curl -fsS "http://localhost:8080/api/cases/'"$id"'" | python3 -m json.tool'
