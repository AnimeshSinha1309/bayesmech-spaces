# AGENTS

## Android Tooling

- `ANDROID_SDK_ROOT=/home/animesh/Android/Sdk`
  Primary Android SDK root on this machine.

- `ADB=/home/animesh/Android/Sdk/platform-tools/adb`
  Use this explicit path if `adb` is not on `PATH`.

- `JAVA=/usr/bin/java`
  System JDK used successfully here.

- `JAVAC=/usr/bin/javac`
  Compiler path discovered in the environment.

- `GRADLE_BOOTSTRAP=/tmp/gradle-8.7/bin/gradle`
  Temporary Gradle binary used to generate the wrapper when the repo had none.

## Repo Layout

- Android project root: `android/`
- Gradle wrapper: `android/gradlew`
- Local SDK config: `android/local.properties`

## Device Check

- Check connected devices:
  `/home/animesh/Android/Sdk/platform-tools/adb devices -l`

- Device seen during setup:
  `58061FDCR009DA` (`Pixel_10`)

## Standard Commands

- Generate/update the wrapper if it is missing:
  `ANDROID_SDK_ROOT=/home/animesh/Android/Sdk /tmp/gradle-8.7/bin/gradle -p android wrapper`

- Build and install the debug app:
  `ANDROID_SDK_ROOT=/home/animesh/Android/Sdk ./android/gradlew -p android installDebug`

- Build debug APK only:
  `ANDROID_SDK_ROOT=/home/animesh/Android/Sdk ./android/gradlew -p android assembleDebug`

- Launch the app on the connected device:
  `/home/animesh/Android/Sdk/platform-tools/adb shell am start -n spaces.bayesmech.com/.MainActivity`

- Reinstall directly from APK if needed:
  `/home/animesh/Android/Sdk/platform-tools/adb install -r android/app/build/outputs/apk/debug/app-debug.apk`

## Codex Working Rules

- Prefer `./android/gradlew -p android ...` over temporary Gradle paths once the wrapper exists.
- Export or prefix `ANDROID_SDK_ROOT=/home/animesh/Android/Sdk` for Android builds.
- Use the explicit `adb` path above unless `command -v adb` confirms a valid shell path.
- Keep Android-specific files under `android/`, not at repo root.
- Treat `android/local.properties` as machine-local setup, not a tracked project file.
