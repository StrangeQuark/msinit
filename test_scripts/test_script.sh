#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE="$SCRIPT_DIR/matrix-workspace"
REPORT_FILE="$SCRIPT_DIR/test_report.csv"
FAILED_RUNS=0
BACKEND_URL="${MSINIT_BACKEND_URL:-http://localhost:3000}"
MAX_PARALLEL_BUILDS="${MAX_PARALLEL_BUILDS:-3}"

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

START_ORDER=(
  authservice
  emailservice
  fileservice
  vaultservice
  telemetryservice
  gatewayservice
  reactservice
)

MODE="subset"   # subset | must-include
SERVICES=()
INCLUDE_SERVICES=()

echo "services,result" > "$REPORT_FILE"

find_service_folder() {
  find "$WORKSPACE" -maxdepth 1 -mindepth 1 -type d \( -name "$1" -o -name "$1-*" \) -print -quit
}

set_env_variable() {
  local file="$1"
  local name="$2"
  local value="$3"

  if grep -q "^${name}=" "$file"; then
    sed -i "s|^${name}=.*$|${name}=${value}|" "$file"
  fi
}

is_selected() {
  local service="$1"

  for selectedService in "${SELECTED_SERVICES[@]}"; do
    if [ "$selectedService" = "$service" ]; then
      return 0
    fi
  done

  return 1
}

restore_env_files() {
  while IFS= read -r file; do
    cp "$file" "${file%.matrix-base}"
  done < <(find "$WORKSPACE" -type f \( -name ".env.matrix-base" -o -name ".env.test.matrix-base" \))
}

configure_integrations() {
  local service
  local variableName
  local value
  local file

  restore_env_files

  while IFS= read -r file; do
    for service in "${DEFAULT_SERVICES[@]}"; do
      variableName="$(echo "$service" | tr '[:lower:]' '[:upper:]')_INTEGRATION"
      value=false

      if is_selected "$service"; then
        value=true
      fi

      set_env_variable "$file" "$variableName" "$value"
      set_env_variable "$file" "VITE_${variableName}" "$value"
    done
  done < <(find "$WORKSPACE" -type f \( -name ".env" -o -name ".env.test" \))
}

docker_cleanup() {
  for folder in "$WORKSPACE"/*; do
    if [ -d "$folder" ] && [ -f "$folder/docker-compose.yml" ]; then
      (
        cd "$folder"
        docker compose down -v --remove-orphans || true
      )
    fi
  done
}

cleanup() {
  docker_cleanup
  rm -rf "$WORKSPACE"
}

wait_for_healthy() {
  local containerName="$1"
  local timeout=300
  local start
  local status

  start=$(date +%s)

  while true; do
    status=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$containerName" 2>/dev/null || true)

    if [ "$status" = "healthy" ]; then
      return 0
    fi

    if (( $(date +%s) - start > timeout )); then
      echo "Timed out waiting for $containerName"
      return 1
    fi

    sleep 2
  done
}

start_service() {
  local service="$1"
  local folder
  local envFile=".env"

  folder=$(find_service_folder "$service")

  if [ -z "$folder" ]; then
    echo "ERROR: Could not find $service in $WORKSPACE"
    return 1
  fi

  if [ "$service" = "authservice" ] || [ "$service" = "gatewayservice" ]; then
    envFile=".env.test"
  fi

  (
    cd "$folder"

    if [ "$service" = "reactservice" ]; then
      docker compose --env-file "$envFile" up -d --build
    else
      docker compose --env-file "$envFile" up -d --no-build
    fi
  )
}

start_selected_services() {
  local service
  local containerName

  for service in "${START_ORDER[@]}"; do
    if is_selected "$service"; then
      start_service "$service"
    fi
  done

  for service in "${START_ORDER[@]}"; do
    if is_selected "$service"; then
      containerName="${service%service}-service"
      wait_for_healthy "$containerName"
    fi
  done
}

run_testservice() {
  local folder
  local exitCode=0

  folder=$(find_service_folder "testservice")

  if [ -z "$folder" ]; then
    echo "ERROR: Could not find testservice in $WORKSPACE"
    return 1
  fi

  (
    cd "$folder"
    docker compose up --no-build --abort-on-container-exit
  ) || exitCode=$?

  (
    cd "$folder"
    docker compose logs --no-color > test-service.log
    docker compose down -v
  )

  return "$exitCode"
}

start_logger() {
  local folder

  if ! is_selected "loggerservice"; then
    return 0
  fi

  folder=$(find_service_folder "loggerservice")

  (
    cd "$folder"
    docker compose up -d --no-build
  )

  wait_for_healthy "logger-service"
}

prepare_images() {
  local service
  local folder
  local runningBuilds=0

  for service in "${DEFAULT_SERVICES[@]}" testservice; do
    folder=$(find_service_folder "$service")

    if [ -n "$folder" ]; then
      (
        cd "$folder"
        docker compose build
      ) &

      runningBuilds=$((runningBuilds + 1))

      if [ "$runningBuilds" -ge "$MAX_PARALLEL_BUILDS" ]; then
        wait -n
        runningBuilds=$((runningBuilds - 1))
      fi
    fi
  done

  wait
}

download_fixture() {
  local repositories=()
  local service
  local payload

  for service in "${DEFAULT_SERVICES[@]}"; do
    repositories+=("{\"repo\":\"$service\",\"branch\":\"main\"}")
  done

  repositories+=("{\"repo\":\"testservice\",\"branch\":\"main\"}")
  payload=$(IFS=,; echo "${repositories[*]}")

  mkdir -p "$WORKSPACE"

  curl -fsS -X POST "$BACKEND_URL/batch-download" \
    -H "Content-Type: application/json" \
    -d "{
      \"projectGroup\":\"com.example\",
      \"javaVersion\":\"21\",
      \"OS\":\"linux\",
      \"repositories\":[$payload]
    }" \
    -o "$WORKSPACE/out.zip"

  unzip -q "$WORKSPACE/out.zip" -d "$WORKSPACE"
  rm "$WORKSPACE/out.zip"

  while IFS= read -r file; do
    cp "$file" "$file.matrix-base"
  done < <(find "$WORKSPACE" -type f \( -name ".env" -o -name ".env.test" \))
}

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

if [ "$MODE" = "must-include" ]; then
  SERVICES=("${DEFAULT_SERVICES[@]}")
elif [ "${#SERVICES[@]}" -eq 0 ]; then
  SERVICES=("${DEFAULT_SERVICES[@]}")
fi

trap cleanup EXIT INT TERM

rm -rf "$WORKSPACE"
download_fixture
docker network inspect shared-network >/dev/null 2>&1 || docker network create shared-network
prepare_images

SERVICE_COUNT="${#SERVICES[@]}"
TOTAL_COMBINATIONS=$((1 << SERVICE_COUNT))

for ((mask=0; mask<TOTAL_COMBINATIONS; mask++)); do
  SELECTED_SERVICES=()

  for ((i=0; i<SERVICE_COUNT; i++)); do
    if (( mask & (1 << i) )); then
      SELECTED_SERVICES+=("${SERVICES[i]}")
    fi
  done

  if [ "$MODE" = "must-include" ]; then
    for required in "${INCLUDE_SERVICES[@]}"; do
      if ! is_selected "$required"; then
        continue 2
      fi
    done
  fi

  SERVICE_NAMES=("${SELECTED_SERVICES[@]}" testservice)
  SERVICE_STRING=$(IFS=+; echo "${SERVICE_NAMES[*]}")

  echo "========================================"
  echo "Running combination: $SERVICE_STRING"
  echo "========================================"

  docker_cleanup
  configure_integrations

  exitCode=0

  if ! start_selected_services || ! run_testservice || ! start_logger; then
    exitCode=1
  fi

  if [ "$exitCode" -eq 0 ]; then
    echo "$SERVICE_STRING,PASS" >> "$REPORT_FILE"
    echo "Result: PASS"
  else
    echo "$SERVICE_STRING,FAIL" >> "$REPORT_FILE"
    echo "Result: FAIL"
    FAILED_RUNS=$((FAILED_RUNS + 1))
  fi

  echo "Running cleanup after combination: $SERVICE_STRING"
  docker_cleanup
done

echo "========================================"
echo "All combinations completed"
echo "Report saved to: $REPORT_FILE"
echo "Failed combinations: $FAILED_RUNS"
echo "========================================"

if [ "$FAILED_RUNS" -ne 0 ]; then
  exit 1
fi
