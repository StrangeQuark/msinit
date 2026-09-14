#!/bin/bash

networks=("shared-network")

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
    local env_file="$2"
    local term
    local command="docker-compose up --build"
    term=$(find_terminal_linux) || {
        echo "No supported terminal emulator found."
        return 1
    }

    if [ -n "$env_file" ]; then
        command="docker-compose --env-file $env_file up --build"
    fi

    echo "Launching docker-compose in $folder using $term"

    case "$term" in
        gnome-terminal|mate-terminal)
            "$term" -- bash -c "cd '$folder' && $command" &
            ;;
        konsole)
            "$term" -e bash -c "cd '$folder' && $command" &
            ;;
        xfce4-terminal|lxterminal|xterm|x-terminal-emulator)
            "$term" -e bash -c "cd '$folder' && $command" &
            ;;
        *)
            echo "Terminal $term is not fully supported."
            ;;
    esac
}

run_compose_macos() {
    local folder="$1"
    local env_file="$2"
    local command="docker-compose up --build"

    if [ -n "$env_file" ]; then
        command="docker-compose --env-file $env_file up --build"
    fi

    echo "Launching docker-compose in $folder using macOS Terminal"
    osascript <<EOF
tell application "Terminal"
    do script "cd '$folder' && $command; exit"
end tell
EOF
}

run_compose() {
    local folder="$1"
    local env_file="$2"

    if [ "$system" = "Darwin" ]; then
        run_compose_macos "$folder" "$env_file"
    elif [ "$system" = "Linux" ]; then
        run_compose_linux "$folder" "$env_file"
    else
        echo "Unsupported OS: $system"
    fi
}

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

        local state
        state=$(docker inspect --format='{{.State.Health.Status}}' "$container" 2>/dev/null)

        if [ "$state" = "healthy" ]; then
            echo "$container is healthy."
            return 0
        fi

        if (( $(date +%s) - start > timeout )); then
            echo "Timed out waiting for container '$container'."
            return 1
        fi

        sleep 2
    done
}

find_service_folder() {
    find "$base_dir" -maxdepth 1 -mindepth 1 -type d \( -name "$1" -o -name "$1-*" \) -print -quit
}

teardown() {
    for folder in "$base_dir"/*; do
        if [ -d "$folder" ] && [ -f "$folder/docker-compose.yml" ]; then
            (
                cd "$folder"
                docker-compose down -v
            )
        fi
    done
}

base_dir=$(pwd)
system=$(uname)
testservice_folder=$(find_service_folder "testservice")
authservice_folder=$(find_service_folder "authservice")
gatewayservice_folder=$(find_service_folder "gatewayservice")
loggerservice_folder=$(find_service_folder "loggerservice")
telemetryservice_folder=$(find_service_folder "telemetryservice")

if [ -n "$(find_service_folder "jenkinsservice")" ]; then
    networks+=("jenkins-network")
fi

create_networks

for folder in "$base_dir"/*; do
    if [ -d "$folder" ] && [ -f "$folder/docker-compose.yml" ]; then
        if [ "$folder" = "$testservice_folder" ] || [ "$folder" = "$loggerservice_folder" ]; then
            continue
        fi

        if [ -n "$testservice_folder" ] && { [ "$folder" = "$authservice_folder" ] || [ "$folder" = "$gatewayservice_folder" ]; }; then
            run_compose "$folder" ".env.test"
        else
            run_compose "$folder"
        fi
    fi
done

if [ -z "$testservice_folder" ]; then
    [ -n "$loggerservice_folder" ] && run_compose "$loggerservice_folder"
    exit 0
fi

for service in auth email file vault react gateway; do
    if [ -n "$(find_service_folder "${service}service")" ]; then
        wait_for_healthy "${service}-service" || exit 1
    fi
done

cd "$testservice_folder"
docker-compose up --build --abort-on-container-exit
exit_code=$?
docker-compose logs --no-color > test-service.log
docker-compose down -v

if [ "$exit_code" -ne 0 ]; then
    echo "Testservice FAILED with exit code $exit_code"
    teardown
    exit "$exit_code"
fi

[ -n "$loggerservice_folder" ] && run_compose "$loggerservice_folder"

[ -n "$loggerservice_folder" ] && wait_for_healthy "logger-service"

echo "Testservice passed."
