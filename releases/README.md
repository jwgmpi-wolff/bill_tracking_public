# MedBill Tracker Release Notes

This public template does not ship signed APK binaries.

Create your own release package:

1. Build APK from `android/` with `./gradlew assembleRelease`.
2. Copy artifact from `android/app/build/outputs/apk/release/app-release.apk`.
3. Rename to a versioned file like `MedBillTracker-vX.Y.Z.apk`.
4. Add checksums with `Get-FileHash -Algorithm SHA256`.
