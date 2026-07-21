#!/usr/bin/env sh
set -eu

port=${SERVER_PORT:-8082}
wget --quiet --timeout=3 --tries=1 --spider "http://127.0.0.1:${port}/actuator/health/readiness"
