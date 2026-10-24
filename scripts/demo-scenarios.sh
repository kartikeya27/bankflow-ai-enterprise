#!/usr/bin/env bash
set -euo pipefail
BASE="${BASE_URL:-http://localhost:8080}"
for CASE in low high hold; do
  REF="${CASE^^}-$(date +%s)-${RANDOM}"
  case "$CASE" in
    low) AMT=400; NEW=false; SANCTIONS=false; MISMATCH=false ;;
    high) AMT=15000; NEW=true; SANCTIONS=false; MISMATCH=true ;;
    hold) AMT=300; NEW=false; SANCTIONS=true; MISMATCH=false ;;
  esac
  curl -fsS -X POST "$BASE/api/cases" -H 'Content-Type: application/json' \
     -d '{"reference":"'"$REF"'","amount":'"$AMT"',"currency":"USD","newBeneficiary":'"$NEW"',"sanctionsHit":'"$SANCTIONS"',"accountMismatch":'"$MISMATCH"',"narrative":"Synthetic case"}'
  printf '\n'
done
