#!/bin/bash

# --- CONFIG ---
networks=(
    "shared-network"
    "authdb-network"
    "emaildb-network"
    "filedb-network"
    "vaultdb-network"
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
        echo "❌ No supported terminal emulator found."
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
            echo "❌ Terminal $term is not fully supported."
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

# --- MAIN ---
create_networks

base_dir=$(pwd)
system=$(uname)

for folder in "$base_dir"/*; do
    if [ -d "$folder" ] && [ -f "$folder/docker-compose.yml" ]; then
        if [ "$system" = "Darwin" ]; then
            run_compose_macos "$folder"
        elif [ "$system" = "Linux" ]; then
            run_compose_linux "$folder"
        else
            echo "Unsupported OS: $system"
        fi
    fi
done
