.PHONY: help build build-release clean install lint test docker-build docker-shell setup

# Default target
help:
	@echo "Available targets:"
	@echo "  setup         - Download Gradle wrapper (first time setup)"
	@echo "  build         - Build debug APK using Docker"
	@echo "  build-release - Build release APK using Docker"
	@echo "  clean         - Clean build artifacts"
	@echo "  lint          - Run lint checks"
	@echo "  test          - Run unit tests"
	@echo "  docker-build  - Build Docker image"
	@echo "  docker-shell  - Open shell in Docker container"
	@echo "  install       - Install debug APK to connected device (requires adb)"

# Docker image name
DOCKER_IMAGE := interval-shuffter-builder
DOCKER_RUN := docker run --rm -v $(PWD):/app -w /app $(DOCKER_IMAGE)

# First time setup - download gradle wrapper
setup:
	@echo "Downloading Gradle wrapper..."
	@mkdir -p gradle/wrapper
	@curl -sL https://services.gradle.org/distributions/gradle-8.2-bin.zip -o /tmp/gradle.zip
	@unzip -q -o /tmp/gradle.zip -d /tmp
	@cp /tmp/gradle-8.2/lib/gradle-wrapper.jar gradle/wrapper/ 2>/dev/null || \
		curl -sL https://raw.githubusercontent.com/gradle/gradle/v8.2.0/gradle/wrapper/gradle-wrapper.jar -o gradle/wrapper/gradle-wrapper.jar
	@rm -rf /tmp/gradle.zip /tmp/gradle-8.2
	@echo "Gradle wrapper setup complete"

# Build Docker image
docker-build:
	@echo "Building Docker image..."
	docker build -t $(DOCKER_IMAGE) .

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
		$(DOCKER_RUN) ./gradlew clean --no-daemon; \
	fi
	rm -rf app/build build .gradle

# Run lint
lint: docker-build
	@echo "Running lint..."
	$(DOCKER_RUN) ./gradlew lint --no-daemon

# Run tests
test: docker-build
	@echo "Running tests..."
	$(DOCKER_RUN) ./gradlew test --no-daemon

# Open shell in Docker container
docker-shell: docker-build
	@echo "Opening shell in Docker container..."
	docker run --rm -it -v $(PWD):/app -w /app $(DOCKER_IMAGE) /bin/bash

# Install APK to connected device (requires local adb)
install: build
	@echo "Installing APK to device..."
	adb install -r app/build/outputs/apk/debug/app-debug.apk
