# Android Client

This Android project is a Jetpack Compose starter for MedBill Tracker.

## Setup

1. Open this `android` folder in Android Studio.
2. Set backend URL with either:
	- environment variable `MEDBILL_API_BASE_URL`, or
	- Gradle property `MEDBILL_API_BASE_URL`.
	If omitted, the app defaults to `http://10.0.2.2:5199/`.
3. Configure Entra auth values in `app/build.gradle.kts` (`ENTRA_CLIENT_ID`, `ENTRA_API_SCOPE`).
4. Update `app/src/main/res/raw/auth_config_single_account.json` with your Entra app registration.
5. Ensure the Entra app redirect URI includes `msauth://com.medbill.tracker/auth`.
6. Sync Gradle and run on device/emulator.

### Command line validation prerequisites

- Java 17 on PATH.
- Android SDK installed and configured with `ANDROID_HOME` or `ANDROID_SDK_ROOT`.
- Gradle wrapper files (`gradlew`, `gradlew.bat`) in this folder.
- Signing env vars for release checks:
	- `ANDROID_KEYSTORE_PATH`
	- `ANDROID_KEYSTORE_PASSWORD`
	- `ANDROID_KEY_ALIAS`
	- `ANDROID_KEY_PASSWORD`

Example:

```powershell
Set-Location ..
./scripts/validate-android.ps1 -RunBuild -AndroidSdkDir "C:\Android\Sdk"
```

## Security expectations

- Do not log PHI or payment-sensitive fields.
- Use OAuth/OIDC sign-in and token storage in encrypted preferences.
- Use hosted payment checkout only.

## Notes

- The UI includes: Sign In, Dashboard, Bills, Add Bill, Bill Detail, Payment, Documents, and Settings.
- Payment screen only opens provider-hosted checkout URLs returned by backend.
