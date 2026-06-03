#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

if command -v docker >/dev/null 2>&1; then
  docker compose up -d postgres
fi

mvn -pl solvia-domain,solvia-application,solvia-infrastructure-persistence -am install
mvn -pl solvia-backend spring-boot:run
