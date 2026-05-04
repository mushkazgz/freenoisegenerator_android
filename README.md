# Free Noise Generator

Native Android app for playing deep brown ambient noise.

## Features

- Kotlin + Jetpack Compose.
- Dark, single-screen interface.
- Procedural audio generation with `AudioTrack`; no bundled audio files.
- Background playback through a `ForegroundService`.
- Large play/pause control with persistent volume, bass and low mids settings.
- Low mids slider is visually 0-100%, but internally capped at 10% for a darker sound.
- Timer slider from off to 12 hours in 30 minute steps.
- Timer changes are applied 5 seconds after releasing the slider.
- Live timer countdown while playback is running.
- Playback stops automatically when the countdown reaches zero.
- Persistent media notification with compact pause and stop actions.
- Launcher icon and notification status icon based on the Free Noise Generator branding.
- Idle screen dimming after 10 seconds without touch while the app is open.

## Audio Model

The app starts with white noise and shapes it into a deep brown profile with two filter bands:

- Bass: 32 Hz high-pass, 125 Hz band-pass, 500 Hz low-pass.
- Low mids: 500 Hz high-pass, 1000 Hz band-pass, 2000 Hz low-pass.

The output gain is boosted by 50% over the original default so the app has more headroom at full volume.

## Run From Android Studio

1. Open this folder in Android Studio.
2. Let Android Studio sync Gradle.
3. Select an emulator or connected Android phone.
4. Press Run.

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
