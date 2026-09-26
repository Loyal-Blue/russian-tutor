#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [ -x ./gradlew ]; then ./gradlew :app:assembleDebug; elif command -v gradle >/dev/null 2>&1; then gradle :app:assembleDebug; else echo "Gradle is required." >&2; exit 1; fi
echo "APK: app/build/outputs/apk/debug/app-debug.apk"