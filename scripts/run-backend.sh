#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -pl solvia-backend -am spring-boot:run
