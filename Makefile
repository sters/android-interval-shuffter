.PHONY: help build build-release clean install lint lint-all ktlint ktlint-format detekt test docker-build docker-shell setup \
        firebase-login firebase-init firebase-distribute firebase-add-testers

# Default target
help:
	@echo "Available targets:"
	@echo ""
	@echo "  Build:"
	@echo "    build           - Build debug APK using Docker"
	@echo "    build-release   - Build release APK using Docker"
	@echo "    clean           - Clean build artifacts"
	@echo "    test            - Run unit tests"
	@echo ""
	@echo "  Lint:"
	@echo "    lint            - Run Android lint"
	@echo "    lint-all        - Run all linters (ktlint + detekt + Android lint)"
	@echo "    ktlint          - Run ktlint check"
	@echo "    ktlint-format   - Auto-format code with ktlint"
	@echo "    detekt          - Run detekt static analysis"
	@echo ""
	@echo "  Docker:"
	@echo "    docker-build    - Build Docker image"
	@echo "    docker-shell    - Open shell in Docker container"
	@echo ""
	@echo "  Device:"
	@echo "    install         - Install debug APK to connected device (requires adb)"
	@echo ""
	@echo "  Firebase App Distribution:"
	@echo "    firebase-login       - Login to Firebase"
	@echo "    firebase-init        - Initialize Firebase project (interactive)"
	@echo "    firebase-distribute  - Build and distribute APK to testers"
	@echo "    firebase-add-testers - Add testers to the group"
	@echo ""
	@echo "  First time setup:"
	@echo "    1. make firebase-login"
	@echo "    2. make firebase-init"
	@echo "    3. Edit firebase.json with your App ID"
	@echo "    4. make firebase-add-testers EMAILS='user1@example.com,user2@example.com'"
	@echo "    5. make firebase-distribute"

# Docker image name
DOCKER_IMAGE := interval-shuffter-builder
DOCKER_RUN := docker run --rm --platform linux/amd64 -v $(PWD):/app -w /app $(DOCKER_IMAGE)

# Firebase settings
FIREBASE_APP_ID ?= $(shell cat firebase.json 2>/dev/null | grep -o '"appId"[[:space:]]*:[[:space:]]*"[^"]*"' | sed 's/.*"appId"[[:space:]]*:[[:space:]]*"\([^"]*\)"/\1/')
FIREBASE_GROUPS ?= testers
RELEASE_NOTES ?= $(shell git log -1 --pretty=format:'%s')

#------------------------------------------------------------------------------
# Build targets
#------------------------------------------------------------------------------

# Build Docker image
docker-build:
	@echo "Building Docker image..."
	docker build --platform linux/amd64 -t $(DOCKER_IMAGE) .

# Build debug APK
build: docker-build
	@echo "Building debug APK..."
	$(DOCKER_RUN) ./gradlew assembleDebug --no-daemon
	@echo "APK location: app/build/outputs/apk/debug/app-debug.apk"

# Build release APK
build-release: docker-build
	@echo "Building release APK..."
	$(DOCKER_RUN) ./gradlew assembleRelease --no-daemon
	@echo "APK location: app/build/outputs/apk/release/app-release-unsigned.apk"

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	@if docker images -q $(DOCKER_IMAGE) > /dev/null 2>&1; then \
		$(DOCKER_RUN) ./gradlew clean --no-daemon 2>/dev/null || true; \
	fi
	rm -rf app/build build .gradle

# Run Android lint
lint: docker-build
	@echo "Running Android lint..."
	$(DOCKER_RUN) ./gradlew lint --no-daemon

# Run all linters
lint-all: docker-build
	@echo "Running all linters..."
	$(DOCKER_RUN) ./gradlew ktlintCheck detekt lint --no-daemon

# Run ktlint check
ktlint: docker-build
	@echo "Running ktlint..."
	$(DOCKER_RUN) ./gradlew ktlintCheck --no-daemon

# Auto-format code with ktlint
ktlint-format: docker-build
	@echo "Formatting code with ktlint..."
	$(DOCKER_RUN) ./gradlew ktlintFormat --no-daemon

# Run detekt
detekt: docker-build
	@echo "Running detekt..."
	$(DOCKER_RUN) ./gradlew detekt --no-daemon

# Run tests
test: docker-build
	@echo "Running tests..."
	$(DOCKER_RUN) ./gradlew test --no-daemon

# Open shell in Docker container
docker-shell: docker-build
	@echo "Opening shell in Docker container..."
	docker run --rm -it --platform linux/amd64 -v $(PWD):/app -w /app $(DOCKER_IMAGE) /bin/bash

# Install APK to connected device (requires local adb)
install: build
	@echo "Installing APK to device..."
	adb install -r app/build/outputs/apk/debug/app-debug.apk

#------------------------------------------------------------------------------
# Firebase App Distribution targets
#------------------------------------------------------------------------------

# Check if firebase CLI is installed
.firebase-check:
	@which firebase > /dev/null || (echo "Error: Firebase CLI not installed. Run: npm install -g firebase-tools" && exit 1)

# Login to Firebase
firebase-login: .firebase-check
	@echo "Logging in to Firebase..."
	firebase login

# Initialize Firebase project
firebase-init: .firebase-check
	@echo "Initializing Firebase project..."
	@echo "Select 'App Distribution' when prompted."
	firebase init appdistribution
	@echo ""
	@echo "Next steps:"
	@echo "1. Go to Firebase Console: https://console.firebase.google.com/"
	@echo "2. Add your Android app to the project"
	@echo "3. Copy the App ID and update firebase.json"

# Add testers to the group
# Usage: make firebase-add-testers EMAILS='user1@example.com,user2@example.com'
firebase-add-testers: .firebase-check
ifndef EMAILS
	@echo "Usage: make firebase-add-testers EMAILS='user1@example.com,user2@example.com'"
	@exit 1
endif
	@echo "Adding testers to group '$(FIREBASE_GROUPS)'..."
	firebase appdistribution:testers:add --emails $(EMAILS) --group $(FIREBASE_GROUPS)

# Build and distribute APK
firebase-distribute: .firebase-check build
	@if [ -z "$(FIREBASE_APP_ID)" ]; then \
		echo "Error: Firebase App ID not set. Update firebase.json with your App ID."; \
		exit 1; \
	fi
	@echo "Distributing APK to Firebase App Distribution..."
	@echo "App ID: $(FIREBASE_APP_ID)"
	@echo "Groups: $(FIREBASE_GROUPS)"
	@echo "Release notes: $(RELEASE_NOTES)"
	firebase appdistribution:distribute app/build/outputs/apk/debug/app-debug.apk \
		--app $(FIREBASE_APP_ID) \
		--groups "$(FIREBASE_GROUPS)" \
		--release-notes "$(RELEASE_NOTES)"
	@echo ""
	@echo "Distribution complete! Testers will receive an email notification."

# Distribute release APK
firebase-distribute-release: .firebase-check build-release
	@if [ -z "$(FIREBASE_APP_ID)" ]; then \
		echo "Error: Firebase App ID not set. Update firebase.json with your App ID."; \
		exit 1; \
	fi
	@echo "Distributing release APK to Firebase App Distribution..."
	firebase appdistribution:distribute app/build/outputs/apk/release/app-release-unsigned.apk \
		--app $(FIREBASE_APP_ID) \
		--groups "$(FIREBASE_GROUPS)" \
		--release-notes "$(RELEASE_NOTES)"
