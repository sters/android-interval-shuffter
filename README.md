# Android Interval Shuffter

A Kotlin Android app for automated interval photography. Set your interval, choose your camera, and let the app capture photos automatically.

## Features

- **Interval Capture**: Take photos at regular intervals (1-60 seconds)
- **Flexible Stop Conditions**: Stop after a set number of photos, after a duration, or run forever
- **Front/Back Camera Support**: Switch between front and back cameras
- **Background Capture**: Runs as a foreground service, capturing continues even when the screen is off
- **Pause/Resume**: Pause and resume capture sessions without losing progress
- **Live Monitoring**: Real-time display of elapsed time and photo count during capture

## Requirements

- Android 7.0 (API 24) or higher
- Camera permission
- Storage permission (for saving photos)
- Notification permission (Android 13+)

## Installation

### From GitHub Releases

Download the latest APK from the [Releases](https://github.com/sters/android-interval-shuffter/releases) page.

### Build from Source

All builds use Docker for reproducibility.

```bash
# Clone the repository
git clone https://github.com/sters/android-interval-shuffter.git
cd android-interval-shuffter

# Build debug APK
make build

# Build release APK
make build-release

# Install to connected device (requires adb)
make install
```

APK outputs:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

## Usage

1. **Grant Permissions**: On first launch, grant the required camera, storage, and notification permissions
2. **Configure Settings**:
   - Set the capture interval (1-60 seconds)
   - Choose front or back camera
   - Select stop condition:
     - **Count**: Stop after capturing N photos
     - **Duration**: Stop after N seconds
     - **Forever**: Continue until manually stopped
   - Optionally keep screen on during capture
3. **Start Capture**: Tap the start button to begin
4. **Monitor Progress**: View live elapsed time and photo count
5. **Control**: Pause, resume, or stop the capture at any time

Photos are saved to `Pictures/IntervalShuffter/` on your device.

## Development

### Build Commands

```bash
make build           # Build debug APK
make build-release   # Build release APK
make lint            # Run lint checks
make test            # Run unit tests
make docker-shell    # Interactive shell for debugging
make clean           # Clean build artifacts
```

### Architecture

This app uses MVVM architecture with Jetpack Compose:

- **CaptureService**: Foreground service for background photo capture
- **CaptureViewModel**: State management for settings and capture status
- **CameraManager**: CameraX wrapper for camera operations
- **ImageSaver**: MediaStore API abstraction for saving photos

### Tech Stack

- UI: Jetpack Compose with Material3
- Camera: CameraX 1.3.0
- Permissions: Accompanist Permissions 0.32.0
- Build: Gradle Kotlin DSL, Android SDK 34 (min 24)

## License

MIT License - see [LICENSE](LICENSE) for details.
