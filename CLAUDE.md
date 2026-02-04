# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

All builds use Docker for reproducibility. The Makefile provides these targets:

```bash
make build           # Build debug APK
make build-release   # Build release APK
make lint            # Run lint checks
make test            # Run unit tests
make install         # Install debug APK to connected device (requires adb)
make docker-shell    # Interactive shell for debugging
make clean           # Clean build artifacts
```

APK outputs:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Architecture

This is a Kotlin Android app for automated interval photography using MVVM architecture with Jetpack Compose.

### Core Components

**CaptureService** (`service/CaptureService.kt`) - Foreground service that runs photo capture:
- Uses Handler + Runnable for interval-based scheduling
- Maintains WakeLock to prevent CPU sleep during long captures
- Communicates with MainActivity via local binder and callbacks

**CaptureViewModel** (`viewmodel/CaptureViewModel.kt`) - State management:
- `CaptureSettings`: interval, stop condition (COUNT/DURATION/FOREVER), camera type, screen on flag
- `CaptureState`: isCapturing, isPaused, capturedCount, elapsedSeconds

**CameraManager** (`camera/CameraManager.kt`) - CameraX wrapper for camera operations

**ImageSaver** (`storage/ImageSaver.kt`) - MediaStore API abstraction, saves to `Pictures/IntervalShuffter/`

### Data Flow

```
SettingsScreen → CaptureViewModel → MainActivity → CaptureService
                                                        ↓
CaptureScreen  ← CaptureViewModel ← MainActivity ← (callbacks)
```

### UI Screens

- **SettingsScreen** (`ui/screens/SettingsScreen.kt`): Configuration (interval 1-60s, camera, stop condition)
- **CaptureScreen** (`ui/screens/CaptureScreen.kt`): Live monitoring with pause/resume/stop controls

## Tech Stack

- UI: Jetpack Compose with Material3
- Camera: CameraX 1.3.0
- Permissions: Accompanist Permissions 0.32.0
- Build: Gradle Kotlin DSL, Android SDK 34 (min 24)

## Release Process

Create a GitHub Release with a `v*` tag (e.g., `v1.2.3`) to trigger CI/CD. GitHub Actions builds the APK and distributes to Firebase App Distribution.
