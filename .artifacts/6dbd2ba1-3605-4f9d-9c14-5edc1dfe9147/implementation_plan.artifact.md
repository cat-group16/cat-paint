# Fix Crash on Android 12+ (Tablet Samsung)

The application crashes on startup on Android 12 devices. This is likely due to the experimental `targetSdk` (37) and potential security strictness regarding exported components (AppWidget) on newer Android versions.

## Proposed Changes

### Build Configuration
#### [MODIFY] [build.gradle.kts (app)](file:///D:/android project/catpaint/app/build.gradle.kts)
- Downgrade `compileSdk` and `targetSdk` from 37 to 34 (Android 14) to ensure compatibility with stable APIs.

### Manifest
#### [MODIFY] [AndroidManifest.xml](file:///D:/android project/catpaint/app/src/main/AndroidManifest.xml)
- Add `android:permission="android.permission.BIND_APPWIDGET"` to the AppWidget receiver to comply with security best practices for exported components.

## Verification Plan

### Automated Tests
- Run `./gradlew assembleDebug` to ensure the project still builds correctly after SDK downgrade.

### Manual Verification
- Deploy the app to an Android 12+ device (or emulator) and verify it opens without crashing.
- Verify that the Home Screen Widget still functions correctly (preview, clear, and save).
