# GhostCam

Native Android camera reference-overlay app built with Kotlin, XML views, and CameraX.

## Build APK with GitHub Actions

The workflow is at `.github/workflows/build.yml`. It runs on pushes that change files under `GhostCam/`, or manually through **Actions → Build APK → Run workflow**.

The workflow uses Java 17 and Gradle 8.7 (no Gradle wrapper required). The generated debug APK is uploaded as the `GhostCam-apk` artifact.

## Local build

From this directory, with Android SDK and Gradle 8.7 installed:

```sh
gradle assembleDebug --no-daemon
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## App details

- Package: `com.example.ghostcam`
- minSdk: 29
- targetSdk: 34
- CameraX: 1.3.4
- Reference image is shown only as a translucent preview overlay. Captured images come directly from CameraX ImageCapture and do not include the overlay.
- Photos are saved to `Pictures/GhostCam`.
