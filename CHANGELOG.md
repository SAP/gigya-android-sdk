# Changelog

All notable changes to the SAP CDC (Gigya) Android SDK are documented here.

Release tag convention: `<module>-v<semver>` (e.g. `core-v7.4.1`, `bio-v2.2.0`).

---

## Unreleased — Maintenance Mode (Phase 1 dependency updates)

### sdk-core
- `androidx.credentials` bumped `1.3.0` → `1.6.0`
- `compileSdk` / `targetSdk` bumped `34` → `35`
- `lifecycle-extensions` dependency removed (was declared but never used)
- `FidoApiServiceV23Impl` marked `@Deprecated` — Google FIDO2 API (`play-services-fido`) no longer actively supported; use `PasskeyAuthenticationProvider` via Credential Manager for new flows
- Kotlin bumped `1.9.20` → `2.1.21` (repo-wide); artifacts now embed Kotlin metadata 2.x — Kotlin 2.x recommended for consuming apps

### sdk-auth
- `firebase-messaging` bumped `20.3.0` → `25.1.1`
- `localbroadcastmanager:1.1.0` declared explicitly (was leaking transitively from firebase 20.x)
- `appcompat` bumped `1.2.0` → `1.7.1`
- `compileSdk` / `targetSdk` bumped `34` → `35`

### sdk-tfa
- `firebase-messaging` bumped `20.3.0` → `25.1.1`
- `localbroadcastmanager:1.1.0` declared explicitly
- `appcompat` bumped `1.2.0` → `1.7.1`
- `compileSdk` / `targetSdk` bumped `34` → `35`

### sdk-biometric
- Unused `compileOnly appcompat` and `compileOnly material` declarations removed
- `compileSdk` / `targetSdk` bumped `34` → `35`

### sdk-nss
- `compileSdk` / `targetSdk` bumped `34` → `35`
- `flutter_debug` / `flutter_release` pinned to `1.9.9` (removed `+` dynamic version)

---

## [bio-v2.2.0] — 2026-07-22

### sdk-biometric
- Migrated from split v23/v28 fingerprint implementation to Jetpack `BiometricPrompt`
- Fixed `BiometricKey.getDecryptionCipher()` returning `null` — now throws `EncryptionException`
- **Breaking change for host apps:** `optIn` / `optInForBiometricSessionLocking` now require `FragmentActivity` instead of `Activity`
- `setAnimationForPrePieDevices(boolean)` deprecated — animation only applied to the removed legacy pre-Pie dialog; method is now a no-op

---

## [core-v7.4.1] — 2024

### sdk-core
- Added support for LINE login with email (idToken support)

---

## [core-v7.4.0] — 2024

### sdk-core
- Added custom identifier login option

---

## [nss-v1.9.12] — 2024

### sdk-nss
- Fixed FlutterEngine cache race condition on activity restart

---

## [tfa-v2.1.1] — 2023

### sdk-tfa
- Added missing Link account v2 parsing fields
- Added `setFileChooser` / `javaScriptEnabled` to `WebViewConfig` option

---

## [auth-v2.2.0] / [tfa-v2.1.0] — 2022

### sdk-auth / sdk-tfa
- Pending intent immutability fix for Android S+

---

## Release tag convention

Tags follow the pattern `<module>-v<semver>`:

| Module | Tag prefix | Example |
|---|---|---|
| sdk-core | `core-v` | `core-v7.4.1` |
| sdk-biometric | `bio-v` | `bio-v2.2.0` |
| sdk-auth | `auth-v` | `auth-v2.2.0` |
| sdk-tfa | `tfa-v` | `tfa-v2.1.1` |
| sdk-nss | `nss-v` | `nss-v1.9.12` |
