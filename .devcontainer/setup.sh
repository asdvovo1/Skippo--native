#!/usr/bin/env bash
# One-shot setup for GitHub Codespaces (or any Debian/Ubuntu box).
# Installs the Android command line SDK + platform 34 + build tools, then
# generates the Gradle wrapper so `./gradlew assembleDebug` just works.
set -euo pipefail

SDK_ROOT="${ANDROID_HOME:-/usr/local/android-sdk}"
CMDLINE_VER="11076708"
GRADLE_VER="8.7"

GOOG_HOST="https://dl.google.com/android/repository"
GRADLE_HOST="https://services.gradle.org/distributions"

sudo_if() { if [ "$(id -u)" -eq 0 ]; then "$@"; else sudo "$@"; fi; }

echo "==> 1/5  base packages"
sudo_if apt-get update -qq
sudo_if apt-get install -y -qq unzip curl zip >/dev/null

echo "==> 2/5  Android command line tools"
if [ ! -d "$SDK_ROOT/cmdline-tools/latest" ]; then
	sudo_if mkdir -p "$SDK_ROOT/cmdline-tools"
	sudo_if chown -R "$(id -u):$(id -g)" "$SDK_ROOT"
	tmp=$(mktemp -d)
	curl -fsSL -o "$tmp/cmdline.zip" "$GOOG_HOST/commandlinetools-linux-${CMDLINE_VER}_latest.zip"
	unzip -q "$tmp/cmdline.zip" -d "$tmp"
	mv "$tmp/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
	rm -rf "$tmp"
fi

export ANDROID_HOME="$SDK_ROOT"
export ANDROID_SDK_ROOT="$SDK_ROOT"
export PATH="$SDK_ROOT/cmdline-tools/latest/bin:$SDK_ROOT/platform-tools:$PATH"

echo "==> 2.5/5  Java 17  (Gradle $GRADLE_VER cannot read Java 22+ version strings)"
find_jdk17() {
	for d in /usr/lib/jvm/*17* /usr/lib/jvm/temurin-17* /opt/java/openjdk-17 /opt/java/openjdk; do
		if [ -x "$d/bin/javac" ] && "$d/bin/java" -version 2>&1 | grep -q '"17'; then echo "$d"; return 0; fi
	done
	return 1
}
JDK17="$(find_jdk17 || true)"
if [ -z "$JDK17" ]; then
	sudo_if apt-get install -y -qq openjdk-17-jdk-headless >/dev/null 2>&1 || true
	JDK17="$(find_jdk17 || true)"
fi
if [ -n "$JDK17" ]; then
	export JAVA_HOME="$JDK17"
	export PATH="$JAVA_HOME/bin:$PATH"
	echo "    JAVA_HOME=$JAVA_HOME"
else
	echo "    !! no JDK 17 found - the build will fail on JDK 22+"
fi

echo "==> 3/5  SDK packages + licences"
yes | sdkmanager --licenses >/dev/null 2>&1 || true
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0" >/dev/null

echo "==> 4/5  Gradle $GRADLE_VER"
if [ ! -x "/opt/gradle/gradle-${GRADLE_VER}/bin/gradle" ]; then
	tmp=$(mktemp -d)
	curl -fsSL -o "$tmp/gradle.zip" "$GRADLE_HOST/gradle-${GRADLE_VER}-bin.zip"
	sudo_if mkdir -p /opt/gradle
	sudo_if unzip -q -o "$tmp/gradle.zip" -d /opt/gradle
	rm -rf "$tmp"
fi
export PATH="/opt/gradle/gradle-${GRADLE_VER}/bin:$PATH"

echo "==> 5/5  Gradle wrapper + local.properties"
cd "$(dirname "$0")/.."
# AGP needs to know where the SDK is when you run ./gradlew from a fresh shell
printf 'sdk.dir=%s\n' "$SDK_ROOT" > local.properties
if [ ! -f gradlew ]; then
	gradle wrapper --gradle-version "$GRADLE_VER" --distribution-type bin
fi
chmod +x gradlew || true

SHELL_RC="$HOME/.bashrc"
if ! grep -q ANDROID_HOME "$SHELL_RC" 2>/dev/null; then
	{
		echo "export ANDROID_HOME=$SDK_ROOT"
		echo "export ANDROID_SDK_ROOT=$SDK_ROOT"
		[ -n "${JDK17:-}" ] && echo "export JAVA_HOME=$JDK17"
		echo "export PATH=\$PATH:$SDK_ROOT/platform-tools:$SDK_ROOT/cmdline-tools/latest/bin:/opt/gradle/gradle-${GRADLE_VER}/bin"
	} >> "$SHELL_RC"
fi

# pin the Gradle daemon to JDK 17 even if the shell later points at a newer JDK
if [ -n "${JDK17:-}" ]; then
	mkdir -p "$HOME/.gradle"
	touch "$HOME/.gradle/gradle.properties"
	if ! grep -q '^org.gradle.java.home=' "$HOME/.gradle/gradle.properties"; then
		echo "org.gradle.java.home=$JDK17" >> "$HOME/.gradle/gradle.properties"
	fi
fi

echo
echo "DONE.  Next:   ./gradlew assembleDebug"
echo "APK ->         app/build/outputs/apk/debug/app-debug.apk"
