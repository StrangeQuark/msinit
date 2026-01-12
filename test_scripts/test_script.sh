#!/bin/bash

set -euo pipefail

ROOT_DIR="$(pwd)"
KEEP_PATH="$ROOT_DIR/test_script.sh"
REPORT_FILE="$ROOT_DIR/test_report.csv"
FAILED_RUNS=0

# Initialize report
echo "services,result" > "$REPORT_FILE"

docker_cleanup() {
  echo "Starting Docker cleanup..."

  for folder in "$ROOT_DIR"/*; do
    if [ -d "$folder" ] && [ -f "$folder/docker-compose.yml" ]; then
      echo "Stopping services in $folder"
      (
        cd "$folder"
        docker compose down -v --remove-orphans --rmi local || true
      )
    fi
  done

  # Hard cleanup to ensure no leftovers
  docker container prune -f || true
  docker image prune -af || true
  docker volume prune -f || true
  docker network prune -f || true

  echo "Docker cleanup complete"
}

filesystem_cleanup() {
  echo "Starting filesystem cleanup..."

  if [ ! -f "$KEEP_PATH" ]; then
    echo "ERROR: keep file not found: $KEEP_PATH"
    exit 1
  fi

  for item in "$ROOT_DIR"/* "$ROOT_DIR"/.*; do
    case "$(basename "$item")" in
      .|..) continue ;;
    esac

    if [ "$item" != "$KEEP_PATH" ] && [ "$item" != "$REPORT_FILE" ]; then
      rm -rf "$item"
    fi
  done

  echo "Filesystem cleanup complete"
}

cleanup() {
  docker_cleanup
  filesystem_cleanup
}

# Ensure cleanup always runs
trap cleanup EXIT INT TERM

# --- Begin service combinations loop ---

# Default services
DEFAULT_SERVICES=(
  authservice
  emailservice
  vaultservice
  fileservice
  reactservice
  gatewayservice
  telemetryservice
  loggerservice
)

MODE="subset"   # subset | must-include
SERVICES=()
INCLUDE_SERVICES=()

# Parse arguments
while [ "$#" -gt 0 ]; do
  case "$1" in
    -mi|--must-include)
      MODE="must-include"
      shift
      while [ "$#" -gt 0 ] && [[ "$1" != -* ]]; do
        INCLUDE_SERVICES+=("$1")
        shift
      done
      ;;
    *)
      SERVICES+=("$1")
      shift
      ;;
  esac
done

# Resolve services based on mode
if [ "$MODE" = "must-include" ]; then
  SERVICES=("${DEFAULT_SERVICES[@]}")
else
  if [ "${#SERVICES[@]}" -eq 0 ]; then
    SERVICES=("${DEFAULT_SERVICES[@]}")
  fi
fi

TESTSERVICE='{"repo":"testservice","branch":"main"}'

SERVICE_COUNT="${#SERVICES[@]}"
TOTAL_COMBINATIONS=$((1 << SERVICE_COUNT))

for ((mask=1; mask<TOTAL_COMBINATIONS; mask++)); do
  REPO_JSON=()
  SERVICE_NAMES=()

  for ((i=0; i<SERVICE_COUNT; i++)); do
    if (( mask & (1 << i) )); then
      svc="${SERVICES[i]}"
      REPO_JSON+=("{\"repo\":\"$svc\",\"branch\":\"main\"}")
      SERVICE_NAMES+=("$svc")
    fi
  done

  # Enforce must-include constraints
  if [ "$MODE" = "must-include" ]; then
    for required in "${INCLUDE_SERVICES[@]}"; do
      found=false
      for svc in "${SERVICE_NAMES[@]}"; do
        if [ "$svc" = "$required" ]; then
          found=true
          break
        fi
      done
      [ "$found" = false ] && continue 2
    done
  fi

  # Always include testservice
  REPO_JSON+=("$TESTSERVICE")
  SERVICE_NAMES+=("testservice")

  REPOS_PAYLOAD=$(IFS=,; echo "${REPO_JSON[*]}")
  SERVICE_STRING=$(IFS=+; echo "${SERVICE_NAMES[*]}")

  echo "========================================"
  echo "Running combination: $SERVICE_STRING"
  echo "========================================"

  curl -X POST http://localhost:3000/batch-download \
    -H "Content-Type: application/json" \
    -d "{
      \"projectGroup\":\"com.example\",
      \"javaVersion\":\"21\",
      \"OS\":\"linux\",
      \"repositories\":[${REPOS_PAYLOAD}]
    }" > out.zip

  unzip -o out.zip

  bash launch_script.sh
  exit_code=$?

  if [ "$exit_code" -eq 0 ]; then
    echo "$SERVICE_STRING,PASS" >> "$REPORT_FILE"
    echo "Result: PASS"
  else
    echo "$SERVICE_STRING,FAIL" >> "$REPORT_FILE"
    echo "Result: FAIL (exit code $exit_code)"
    FAILED_RUNS=$((FAILED_RUNS + 1))
  fi

  echo "Running cleanup after combination: $SERVICE_STRING"
  cleanup
done

echo "========================================"
echo "All combinations completed"
echo "Report saved to: $REPORT_FILE"
echo "Failed combinations: $FAILED_RUNS"
echo "========================================"

if [ "$FAILED_RUNS" -ne 0 ]; then
  exit 1
fi
