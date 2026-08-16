# Gigya Android SDK — Example App

The example app is a **developer test harness** for the Gigya Android SDK. It is written in Jetpack Compose + MVVM and covers every major SDK flow: credentials login/register, social login, OTP, TFA, biometric session management, passkeys, push TFA, and push auth.

It is intentionally minimal — not a UI showcase — designed to be easy to read, fork, and extend for SDK evaluation and automated testing.

---

## Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog or later |
| Android SDK | API 34 (compileSdk) |
| Java | 1.8 (project default) |
| Kotlin | 1.9.20 |

---

## Local Setup

The app requires two secret files that are **gitignored** and must be created locally before building.

### 1. `secrets.xml`

Copy the template and fill in your values:

```bash
cp example/secrets.xml.template example/src/main/res/values/secrets.xml
```

Edit `secrets.xml` and replace the placeholder values:

| Key | Description |
|---|---|
| `gigya_api_key` | Your Gigya / SAP CDC site API key |
| `gigya_api_domain` | Your data center (e.g. `us1.gigya.com`) |
| `facebook_app_id` | Facebook App ID (required for Facebook social login) |
| `facebook_client_token` | Facebook Client Token |

### 2. `gigyaSdkConfiguration.json`

Copy the template and fill in your values:

```bash
cp example/gigyaSdkConfiguration.json.template example/src/main/assets/gigyaSdkConfiguration.json
```

Edit the file and replace `YOUR_API_KEY_HERE` and `YOUR_API_DOMAIN_HERE` with the same values used in `secrets.xml`.

### 3. `google-services.json` (optional — required for push TFA/auth)

Place your Firebase project's `google-services.json` in `example/google-services.json`.
Without this file the app builds and runs but push TFA and push auth flows will not work.

---

## Build & Run

```bash
# Build all SDK modules + example app
./gradlew :example:assembleDebug

# Install on a connected device or emulator
./gradlew :example:installDebug
```

---

## Supported Flows

| Flow | Entry point |
|---|---|
| Credentials login | Login screen → email + password |
| Credentials register | Login screen → Register button |
| Social login | Login screen → provider name input |
| OTP phone login | Login screen → OTP Login button |
| SSO login | Login screen → SSO Login button |
| WebAuthn / Passkey login | Login screen → Login with Passkey button |
| TFA (TOTP, phone, email) | Triggered automatically on login interruption |
| Account link | Triggered automatically on conflicting account |
| Get account info | Account screen → Get Account Info |
| Logout | Account screen → Logout |
| Add / remove social connection | Account screen → Connections section |
| Register / revoke passkey | Account screen → Passkeys section |
| Biometric opt-in / opt-out / lock / unlock | Account screen → Biometric section |
| Push TFA opt-in | Account screen → Push Notifications section |
| Push Auth opt-in | Account screen → Push Notifications section |
| SDK re-initialisation | Settings icon (top-right on any screen) |

---

## CI / Automated Runs

Secret files are written automatically at CI build time via the `generateSecrets` Gradle task.
Set the following environment variables in your CI environment:

| Env var | Maps to |
|---|---|
| `GIGYA_API_KEY` | `gigya_api_key` in `secrets.xml` |
| `GIGYA_API_DOMAIN` | `gigya_api_domain` in `secrets.xml` |
| `FACEBOOK_APP_ID` | `facebook_app_id` in `secrets.xml` |
| `FACEBOOK_CLIENT_TOKEN` | `facebook_client_token` in `secrets.xml` |

Then run:

```bash
./gradlew :example:assembleDebug
```

The task runs automatically before `preBuild` — no explicit invocation needed.

---

## Architecture

The app follows Google's recommended Compose + MVVM architecture:

```
ui/<flow>/
  <Flow>Screen.kt       — stateless @Composable + @Preview
  <Flow>ViewModel.kt    — mutableStateOf UiState, calls repository
data/
  IGigyaRepository.kt   — interface (swap for fake in tests)
  GigyaRepository.kt    — production SDK bridge
navigation/
  Screen.kt             — nav route constants
  AppNavGraph.kt        — all destinations wired
ui/common/
  TestTags.kt           — testTag constants for all interactive elements
```

All SDK callbacks are bridged in `GigyaRepository` — ViewModels and Screens are pure Kotlin/Compose with no SDK types leaking through.

See `docs/PHASE3_REWRITE_PLAN.md` for the full architecture decision record.
