#!/usr/bin/env bash
# ==============================================================================
# AcadsCatchUp — Cross-Platform Linux & macOS Launcher
# ==============================================================================
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ -f "$SCRIPT_DIR/app/AcadsCatchUp.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/app/AcadsCatchUp.jar"
elif [ -f "$SCRIPT_DIR/dist/AcadsCatchUp.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/dist/AcadsCatchUp.jar"
elif [ -f "$SCRIPT_DIR/AcadsCatchUp.jar" ]; then
    JAR_PATH="$SCRIPT_DIR/AcadsCatchUp.jar"
else
    echo "[Error] Could not locate AcadsCatchUp.jar!"
    exit 1
fi

echo "=============================================================================="
echo " Starting AcadsCatchUp Desktop Client..."
echo "=============================================================================="

if command -v java >/dev/null 2>&1; then
    java -jar "$JAR_PATH" "$@"
else
    echo "[Error] Java is not installed or not in PATH."
    echo "Please install Java 21 or later:"
    echo "  Ubuntu/Debian: sudo apt install openjdk-21-jre"
    echo "  Fedora/RHEL:   sudo dnf install java-21-openjdk"
    echo "  Arch Linux:    sudo pacman -S jre21-openjdk"
    echo "  macOS:         brew install openjdk@21"
    exit 1
fi
