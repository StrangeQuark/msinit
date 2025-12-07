#!/bin/bash

# --- CONFIG ---
networks=(
    "shared-network"
    "authdb-network" # Integration line: Auth
    "emaildb-network" # Integration line: Email
    "filedb-network" # Integration line: File
    "vaultdb-network" # Integration line: Vault
    "telemetrydb-network" # Integration line: Telemetry
)

# --- FUNCTIONS ---
create_networks() {
    for network in "${networks[@]}"; do
        if ! docker network ls --format '{{.Name}}' | grep -q "^${network}$"; then
            docker network create "$network"
            echo "Created network: $network"
        else
            echo "Network already exists: $network"
        fi
    done
}

find_terminal_linux() {
    # List of possible terminal emulators
    for term in gnome-terminal konsole xfce4-terminal lxterminal mate-terminal xterm x-terminal-emulator; do
        if command -v "$term" >/dev/null 2>&1; then
            echo "$term"
            return 0
        fi
    done
    return 1
}

run_compose_linux() {
    local folder="$1"
    local term
    term=$(find_terminal_linux) || {
        echo "No supported terminal emulator found."
        return 1
    }

    echo "Launching docker-compose in $folder using $term"

    case "$term" in
        gnome-terminal|mate-terminal)
            "$term" -- bash -c "cd '$folder' && docker-compose up --build" &
            ;;
        konsole)
            "$term" -e bash -c "cd '$folder' && docker-compose up --build" &
            ;;
        xfce4-terminal|lxterminal|xterm|x-terminal-emulator)
            "$term" -e bash -c "cd '$folder' && docker-compose up --build" &
            ;;
        *)
            echo "Terminal $term is not fully supported."
            ;;
    esac
}

run_compose_macos() {
    local folder="$1"
    echo "Launching docker-compose in $folder using macOS Terminal"
    osascript <<EOF
tell application "Terminal"
    do script "cd '$folder' && docker-compose up --build; exit"
end tell
EOF
}
# Integration function start: Test
find_container() {
  local keyword="$1"
  docker ps -a --format '{{.Names}}' | grep -i "$keyword" | head -n 1
}

wait_for_healthy() {
  local keyword="$1"
  local timeout=300
  local start
  start=$(date +%s)

  echo "Waiting for container containing keyword '$keyword'..."

  while true; do
    local container
    container=$(find_container "$keyword")

    if [ -z "$container" ]; then
      echo "Container matching '$keyword' not found yet..."
      sleep 2
      continue
    fi

    echo "Container matching '$keyword' was found"

    local state
    state=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null)

    echo "Container matching '$keyword' state is '$state'"

    if [ "$state" = "healthy" ]; then
      echo "$container is healthy."
      return 0
    fi

    if [ -z "$state" ]; then
      echo "$container has no healthcheck or is not ready."
    fi

    local now
    now=$(date +%s)

    if (( now - start > timeout )); then
      echo "Timed out waiting for container '$container' with keyword '$keyword'."
      return 1
    fi

    sleep 2
  done
}
# Integration function end: Test

# --- MAIN ---
create_networks

base_dir=$(pwd)
system=$(uname)

for folder in "$base_dir"/*; do
  if [ -d "$folder" ] && [ -f "$folder/docker-compose.yml" ]; then
    # Integration function start: Test
    if [[ "$folder" == *"testservice"* ]]; then
      testservice_folder="$folder"
      continue
    fi
    # Integration function end: Test
    if [ "$system" = "Darwin" ]; then
      run_compose_macos "$folder"
    elif [ "$system" = "Linux" ]; then
      run_compose_linux "$folder"
    else
      echo "Unsupported OS: $system"
    fi
  fi
done
# Integration function start: Test
if [ -n "$testservice_folder" ]; then
  echo "Waiting for all services to start..."

  wait_for_healthy "auth-service" # Integration line: Auth
  wait_for_healthy "email-service" # Integration line: Email
  wait_for_healthy "file-service" # Integration line: File
  wait_for_healthy "vault-service" # Integration line: Vault
  wait_for_healthy "react-service" # Integration line: React

  echo "Running testservice and capturing output..."
  output=$(cd "$testservice_folder" && docker-compose up --build 2>&1)
  echo "$output"
fi
# Integration function end: Test
