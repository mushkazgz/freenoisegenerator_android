# Play Store Release Checklist

Current release candidate: `1.1.0 (3)`.

## Before Upload

- Confirm final app name: Free Noise Generator.
- Confirm developer display name: Noisyogui.
- Confirm support email: josemanuellasjim@gmail.com.
- Publish the privacy policy from the public GitHub repository: `https://github.com/mushkazgz/freenoisegenerator_android/blob/main/PRIVACY_POLICY.md`.
- Confirm Play Store category: Music & Audio.
- Review all in-app text in English.
- Test on a real phone with wired audio, speaker and Bluetooth audio.
- Test playback with the app minimized.
- Test notification Pause, Play and Stop.
- Test removing the app from recents.
- Test timer values including 0, 30 minutes and a multi-hour timer.
- Test screensaver on/off from Settings.
- Generate final phone screenshots.
- Submit this release before August 31, 2026. Google Play requires API 36 for
  new mobile app submissions and updates from that date.

## Signing Setup

1. Generate an upload keystore outside Git history.
2. Copy `keystore.properties.example` to `keystore.properties`.
3. Fill in the real keystore path, alias and passwords.
4. Keep `keystore.properties` and the keystore file private.
5. Back up the keystore and passwords somewhere safe before uploading to Play Console.

Current backup note: `release-upload.jks` and `keystore.properties` have been backed up to the developer's MEGA account.

Example command:

```powershell
keytool -genkeypair -v -keystore release-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias free-noise-generator-upload
```

## Build Release Bundle

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:Path"
.\gradlew.bat bundleRelease
```

The release bundle is generated at:

```text
app/build/outputs/bundle/release/app-release.aab
```

If `keystore.properties` is missing, Gradle may still generate a bundle for validation, but it will be unsigned and should not be uploaded to Play Console.

## Play Console

- Create the app in Google Play Console.
- Complete App content:
  - Privacy policy.
  - Data safety.
  - Ads: No, unless this changes later.
  - Advertising ID: No. The app does not declare the `AD_ID` permission.
  - App access: No special access required.
  - Content rating questionnaire.
  - Target audience.
  - Foreground service declaration if requested.
- Upload the signed `.aab`.
- Start with internal testing.
- Move to closed testing if required by the developer account.
- Request production access when testing requirements are satisfied.

## Next Platform Update

Before uploading any release on or after August 31, 2026, migrate
`compileSdk` and `targetSdk` from API 35 to API 36 and repeat the real-device
playback, notification, timer, rotation and background-service tests.
