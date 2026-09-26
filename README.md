# Russian Tutor

A Duolingo-inspired Android app for learning Russian with an offline-first AI tutor.

## Features
- Step-by-step Russian course
- Daily progress and lesson path
- AI conversation practice
- AI-generated lessons and exercises
- Three downloadable local GGUF model tiers
- Automatic device-aware model recommendation
- Private runtime model storage
- GitHub Actions APK builds

## Build

Open in Android Studio or run `./scripts/build-apk.sh`.

Every push to main builds a debug APK. A version tag such as v0.1.0 triggers the release workflow.

## Native inference status

The UI, curriculum, model catalog and downloader are implemented. LocalTutor is the adapter boundary for the native GGUF runtime. Wire a llama.cpp Android JNI/AAR implementation into that adapter for real on-device generation.

## Model licensing

Review each model's license and redistribution terms before distributing its weights.