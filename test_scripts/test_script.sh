#!/bin/bash

set -euo pipefail

ROOT_DIR="$(pwd)"
KEEP_PATH="$ROOT_DIR/test_script.sh"

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
    # Skip . and ..
    case "$(basename "$item")" in
      .|..) continue ;;
    esac

    # Skip test_scripts directory (but clean its contents selectively)
    if [ "$item" != "$ROOT_DIR/test_script.sh" ]; then
      rm -rf "$item"
    fi

    # Delete everything else
    # rm -rf "$item"
  done

  echo "Filesystem cleanup complete"
}

cleanup() {
  docker_cleanup
  filesystem_cleanup
}

# Ensure cleanup always runs
trap cleanup EXIT INT TERM

curl -X POST http://localhost:3000/batch-download \
  -H "Content-Type: application/json" \
  -d '{
    "projectGroup":"com.example",
    "javaVersion":"21",
    "OS":"linux",
    "repositories":[
      {"repo":"authservice","branch":"main"},
      {"repo":"emailservice","branch":"main"},
      {"repo":"vaultservice","branch":"main"},
      {"repo":"fileservice","branch":"main"},
      {"repo":"gatewayservice","branch":"main"},
      {"repo":"loggerservice","branch":"main"},
      {"repo":"telemetryservice","branch":"main"},
      {"repo":"reactservice","branch":"main"},
      {"repo":"testservice","branch":"main"}
    ]
  }' > out.zip

unzip -o out.zip

bash launch_script.sh
exit_code=$?

if [ "$exit_code" -eq 0 ]; then
  echo "launch_script.sh succeeded"
else
  echo "launch_script.sh failed with exit code $exit_code"
  exit "$exit_code"
fi
