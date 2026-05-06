# Free Noise Generator

Native Android app for playing deep brown ambient noise.

## Overview

Free Noise Generator is a small, focused Android app built for deep ambient noise, rest, focus and sleep. It generates audio procedurally with `AudioTrack`, so there are no bundled audio files.

The app is intentionally minimal: one main playback screen, persistent controls, foreground playback and a low-light idle screensaver.

## Features

- Kotlin + Jetpack Compose.
- Portrait-only, single-screen Android interface.
- Black and white main palette with a pure black startup/splash background.
- Procedural deep brown noise generation with `AudioTrack`.
- Background playback through a `ForegroundService`.
- Persistent media notification with compact pause and stop actions.
- Large play/pause control.
- Persistent `Volume`, `Bass`, `Low mids` and `Timer` settings.
- `Volume`, `Bass` and `Low mids` apply immediately while dragging.
- `Low mids` is visually 0-100%, but internally capped at 10% for a darker sound.
- Timer from off to 12 hours in 30 minute steps.
- Timer changes are applied 5 seconds after releasing the slider.
- Live timer countdown while playback is running.
- Playback stops automatically when the countdown reaches zero.
- Settings dialog with:
  - Screensaver on/off.
  - Bluetooth shortcut to Android Bluetooth settings.
  - About/info entry.
- About dialog with Noisyogui credit and PayPal donation link.
- Launcher icon and notification status icon based on the Free Noise Generator branding.
- Idle screensaver after 10 seconds without touch.
- Screensaver includes a warm ember/fire glow, stars, constellations, a soft milky-way wash and an occasional shooting star.

## Audio Model

The engine starts with white noise and shapes it into a deep brown profile with two filter bands:

- Bass: 32 Hz high-pass, 125 Hz band-pass, 500 Hz low-pass.
- Low mids: 500 Hz high-pass, 1000 Hz band-pass, 2000 Hz low-pass.

The low mids control is intentionally limited to preserve the dark/brown character of the sound.

The output gain is boosted by 50% over the original default so the app has more headroom at full volume.

## Bluetooth Behavior

The `Bluetooth` button opens Android's native Bluetooth settings directly.

Android does not provide a reliable public API for normal Play Store apps to force-connect A2DP headphones or speakers on current target SDKs. Keeping this as a system-settings shortcut avoids fragile hidden APIs and keeps the app safer for distribution.

## Screensaver Behavior

When the app is open in the foreground and no touch interaction happens for 10 seconds, the idle screensaver fades in.

Any touch interaction wakes the app immediately, including taps, button presses and slider drags. The screensaver can be disabled from `Settings`.

## Run From Android Studio

1. Open this folder in Android Studio.
2. Let Android Studio sync Gradle.
3. Select an emulator or connected Android phone.
4. Press Run.

## Tooling

- Android Gradle Plugin 8.13.2.
- Gradle wrapper 8.13.
- Kotlin 2.0.21.
- Java 17.
- Minimum SDK 26.
- Target SDK 35.

## Build From Terminal

On this machine, Android Studio's bundled JDK can be used:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:Path"
.\gradlew.bat assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install On A Connected Phone

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb devices -l
& $adb install -r app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n com.freenoisegenerator.app/.MainActivity
```

## Documentation Policy

Code comments and documentation should be written in English.

When behavior changes, update this README and add concise code comments only where the implementation is not self-explanatory.
