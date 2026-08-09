# Free Noise Generator

Native Android app for playing deep brown ambient noise.

## Overview

Free Noise Generator is a small, focused Android app built for deep ambient noise, rest, focus and sleep. It generates audio procedurally with `AudioTrack`, so there are no bundled audio files.

The app is intentionally focused: one immersive playback console, persistent controls, foreground playback and a low-light idle screensaver.

## Features

- Kotlin + Jetpack Compose.
- Responsive, edge-to-edge Android interface with portrait and landscape support.
- Responsive analog-console artwork with warm wood, black metal and amber details.
- Unified black, ivory, aged-gold, copper and amber brand palette across app and store icons.
- Subtle 50% white version watermark sourced automatically from the Gradle release version.
- Procedural deep brown noise generation with `AudioTrack`.
- Background playback through a `ForegroundService`.
- Persistent media notification with compact pause and stop actions.
- Active playback continues when the app is minimized or removed from recents.
- Animated rotary controls for `Volume`, `Bass`, `Low mids` and `Timer`.
- Mechanical rotary detents with a strong system-respecting haptic pulse at every step and a double pulse when volume crosses the off position.
- Large touch targets and adjustable accessibility semantics despite the compact artwork.
- Amber playback light that reflects playback state and pulses while audio is active.
- Persistent `Bass` and `Low mids` settings.
- `Volume` always starts at zero so every fresh app launch begins silently in the off position.
- `Timer` always starts at `Off` on a fresh app launch.
- `Volume`, `Bass` and `Low mids` apply immediately while dragging.
- `Low mids` is visually 0-100%, but internally capped at 10% for a darker sound.
- Timer from off to 12 hours in 30 minute steps.
- Timer changes are applied 5 seconds after releasing the rotary control.
- Live timer value and countdown integrated into the console artwork.
- Timer dial rotates continuously toward `Off` as the countdown progresses.
- Playback stops automatically when the countdown reaches zero.
- Settings dialog with:
  - Responsive wooden control-panel artwork matching the main console.
  - Independent amber indicators for selected and pressed controls.
  - Strong haptic feedback on every button, including the close gear.
  - Screensaver on/off.
  - Bluetooth shortcut to Android Bluetooth settings.
  - About/info entry.
- About dialog with Noisyogui credit and PayPal donation link.
- Launcher icon and notification status icon based on the Free Noise Generator branding.
- Idle screensaver after 10 seconds without touch.
- Screensaver combines a fixed pixel-art bonfire scene with procedural flames and rising embers.

## Audio Model

The engine starts with white noise and shapes it into a deep brown profile with two filter bands:

- Bass: 32 Hz high-pass, 125 Hz band-pass, 500 Hz low-pass.
- Low mids: 500 Hz high-pass, 1000 Hz band-pass, 2000 Hz low-pass.

The low mids control is intentionally limited to preserve the dark/brown character of the sound.

The output gain is boosted by 50% over the original default so the app has more headroom at full volume.

## Main Interface

The main screen is rendered from one fixed 1024 x 1535 artwork and four transparent knob sprites. A shared source-coordinate transform positions the background, knobs, playback light and settings hotspot, so every interactive layer stays aligned when the available phone or tablet viewport changes.

Turn a knob naturally by tracing a clockwise or counter-clockwise arc around its face. Rotary controls use angular movement exclusively, so straight horizontal, vertical or radial swipes do not compete with the physical turning gesture. The recognizer handles the angle boundary without jumps and ignores unstable input near the center. Each control snaps to its visual scale: 27 positions for `Volume`, 19 positions for each tone band and 25 half-hour positions for `Timer`. Crossing a position produces a pronounced Android haptic click when system haptics are enabled. Moving `Volume` to zero pauses playback; moving it away from zero starts playback, with the strongest haptic response in both directions. A fresh app launch always resets `Volume` to zero. The audio engine receives `Volume`, `Bass` and `Low mids` changes immediately at every detent; band preferences are persisted when the gesture ends. Rotary movement uses a short visual interpolation without delaying the underlying audio value.

The original static timer numbers are removed from the production artwork because the app timer runs from `Off` to 12 hours. The current duration or live countdown is drawn beneath the timer knob at runtime.

## Settings Interface

The settings screen is a responsive full-screen Compose dialog built from reusable wooden panel, button and indicator assets. Controls remain native touch targets with button and selected-state accessibility semantics; the artwork is decorative and does not contain baked-in labels or state lights. Selected screensaver state is shown by the amber light below `On` or `Off`, while command buttons briefly light and depress before their action runs. The top-right gear is also a large accessible close control. The same proportional layout is used in portrait and landscape without cropping.

## Bluetooth Behavior

The `Bluetooth` button opens Android's native Bluetooth settings directly.

Android does not provide a reliable public API for normal Play Store apps to force-connect A2DP headphones or speakers on current target SDKs. Keeping this as a system-settings shortcut avoids fragile hidden APIs and keeps the app safer for distribution.

## Screensaver Behavior

When the app is open in the foreground and no touch interaction happens for 10 seconds, the idle screensaver fades in and temporarily hides the Android system bars. A fixed bonfire image provides the logs and ambient light while a low-resolution heat simulation produces non-repeating pixel-art flames without video or animated image assets. The procedural layer uses a tapered, drifting mask with intensity-based transparency so the animated flame blends into the logs without rectangular edges. The scene scales from the image width and remains centered in both portrait and landscape layouts.

Any touch interaction wakes the app immediately, including taps, button presses and slider drags. The screensaver can be disabled from `Settings`.

Returning to the app from the background also wakes the interface so the screensaver never stays on after resume.

## Notification Behavior

The notification exposes compact `Pause` and `Stop` controls.

`Pause` stops audio playback but keeps the service notification available with a `Play` action. `Stop` from the notification fully removes foreground playback, clears the notification and closes the app task if the main screen is still alive.

The main in-app playback button only starts or stops audio. It does not close the app.

Minimizing the app or removing its task from Android recents keeps active playback running in the foreground service. Reopening the app restores the live volume, playback light and timer countdown.

If playback is paused when the task is removed, the service and notification close normally. Active playback can always be stopped by moving `Volume` to zero or using the notification `Stop` action.

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
- Version name 1.1.0.
- Version code 3.

## Rebuild Mainframe Assets

The checked-in Android assets are ready to use. To regenerate them from the original `MainFrame.png`, install the small OpenCV helper dependency and run:

```powershell
python -m pip install opencv-python-headless
powershell -ExecutionPolicy Bypass -File .\tools\extract-mainframe-assets.ps1 `
    -Source 'C:\path\to\MainFrame.png'
```

The script copies the background, removes obsolete timer-number glyphs without damaging nearby dial marks, and exports circular transparent sprites for each knob.

## Rebuild Settings Assets

The checked-in settings artwork is ready to use. To rebuild it from replacement panel, button and indicator PNG files, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\prepare-settings-assets.ps1 `
    -Panel 'C:\path\to\settings-panel.png' `
    -SmallButton 'C:\path\to\small-button.png' `
    -WideButton 'C:\path\to\wide-button.png' `
    -Indicator 'C:\path\to\indicator.png'
```

The processor removes generated checkerboard backgrounds, crops transparent margins and exports deterministic `drawable-nodpi` assets at the dimensions expected by the Compose layout. Labels and icons are rendered by the app so they stay sharp and accessible.

## Rebuild Brand Assets

The checked-in launcher and Play Store assets use the same warm palette as the analog console. After updating the 512 px store icon, regenerate every Android launcher density with:

```powershell
powershell -ExecutionPolicy Bypass -File .\tools\rebuild-brand-assets.ps1
```

Android notification icons remain white monochrome vectors because status bar icons must use a system-tinted silhouette.

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

## Release Preparation

Release signing is configured through a private `keystore.properties` file that is intentionally ignored by Git.
The current Play Store release candidate is `1.1.0 (3)`.

1. Copy `keystore.properties.example` to `keystore.properties`.
2. Generate an upload keystore.
3. Fill in the private keystore values.
4. Build the Play Store bundle with:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:Path"
.\gradlew.bat bundleRelease
```

The release Android App Bundle is generated at:

```text
app/build/outputs/bundle/release/app-release.aab
```

Without a private `keystore.properties` file, Gradle can generate the release bundle for validation, but the bundle is unsigned and is not ready for Play Store upload.

Publishing support documents:

- [Privacy Policy](PRIVACY_POLICY.md)
- Public privacy policy URL for Play Console: `https://github.com/mushkazgz/freenoisegenerator_android/blob/main/PRIVACY_POLICY.md`
- [Play Store listing draft](docs/play-store-listing.md)
- [Data Safety draft](docs/data-safety.md)
- [Play Console answers draft](docs/play-console-answers.md)
- [Release notes for 1.1.0](docs/release-notes-1.1.0.md)
- [Verified artifact details for 1.1.0](docs/release-artifact-1.1.0.md)
- [Release checklist](docs/release-checklist.md)
- [Reusable Play metadata](fastlane/metadata/android/en-US)

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
