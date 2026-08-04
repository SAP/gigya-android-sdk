# Gigya Android SDK — Example App

This is a basic example application demonstrating the core flows of the Gigya Android SDK. It is intended as a reference for integration, not a production-ready implementation.

## Supported flows

| Flow | Description |
|---|---|
| Login | Credential, social, SSO, passwordless, OTP |
| Registration | Standard account registration |
| Account | Get account info, add/remove social connections |
| FIDO / Passkeys | Register, revoke, and list passkeys |
| Biometric | Opt-in/out, lock/unlock session |
| TFA | TOTP and phone-based two-factor authentication |
| Push TFA | Opt-in for push-based TFA challenge |
| Push Auth | Opt-in for push-based login approval |
| ScreenSets | Web-based and native screen-sets |
| SSO Exchange | Session exchange via hosted page |

## Setup

### Required local files

The following files are gitignored and must be provided locally before the app can run:

| File | Purpose |
|---|---|
| `example/src/main/assets/gigyaSdkConfiguration.json` | API key and data center for SDK initialization |
| `example/src/main/res/values/secrets.xml` | Facebook app ID and client token |

### Push TFA / Push Auth

Push notification support requires a valid Firebase project. To enable it:

1. Create a project in the [Firebase Console](https://console.firebase.google.com/) and register the app with the package name `com.gigya.android.sample`.
2. Download the generated `google-services.json` file and place it at `example/google-services.json`.
3. Ensure the Firebase project's **Cloud Messaging** service is enabled.

> Without `google-services.json`, the app will crash when the Push TFA or Push Auth opt-in buttons are tapped.

## Notes

- On Android 13+, the app requests the `POST_NOTIFICATIONS` permission at launch. Push flows require this permission to be granted.
- The `GigyaFirebaseMessagingService` is registered in the manifest and handles all incoming push routing automatically.
- This example app uses a debug keystore. Replace with your own signing config for any non-development use.
