FROM openjdk:17-slim

ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV ANDROID_HOME=${ANDROID_SDK_ROOT}
ENV PATH=${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

# Install required packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    wget \
    unzip \
    git \
    && rm -rf /var/lib/apt/lists/*

# Download and install Android command line tools
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools && \
    mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses && \
    sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"

WORKDIR /app

# Copy gradle wrapper files first for caching
COPY gradle/wrapper/gradle-wrapper.properties gradle/wrapper/
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Make gradlew executable and download gradle
RUN chmod +x gradlew

# Copy the rest of the project
COPY . .

# Default command
CMD ["./gradlew", "assembleDebug"]
