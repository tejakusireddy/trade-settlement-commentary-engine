#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

run_step() {
  local name="$1"
  shift
  echo ""
  echo "==> ${name}"
  if "$@"; then
    echo "PASS: ${name}"
  else
    echo "FAIL: ${name}"
    exit 1
  fi
}

echo "Running default release gates (non-Docker): backend unit + frontend build"

run_step "Backend unit gate (non-Docker): mvn -q test" \
  bash -lc "cd \"${ROOT_DIR}\" && mvn -q test"

run_step "Frontend production build gate: npm run -s build" \
  bash -lc "cd \"${ROOT_DIR}/frontend\" && npm run -s build"

echo ""
echo "Integration gate is explicit and Docker-backed (not run by this script):"
echo "  cd \"${ROOT_DIR}\" && mvn -q verify -Pintegration-tests"
echo ""
echo "Expected semantics:"
echo "  - PASS means all configured checks succeeded."
echo "  - FAIL means stop release and remediate before deploy."
