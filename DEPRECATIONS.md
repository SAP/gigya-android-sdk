# Deprecation Ledger

> **Ledger only — no removals.** This file inventories all `@Deprecated` members across the Android SDK modules.
> Removals are deferred to a future explicitly-planned major release.
> Last updated: 2026-09-01

---

## sdk-core

### `ConfigFactory.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `loadFromManifest` | `public Config loadFromManifest()` | Will be removed in SDK version 6. Manifest-based config superseded by `gigyaSdkConfiguration.json` loaded via `loadFromJson()`. |

### `Config.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `accountCacheTime` (field) | `private int accountCacheTime` | Will be removed in SDK version 6. Use `GigyaAccountConfig.getCacheTime()` instead. |
| `setAccountCacheTime` | `public void setAccountCacheTime(int accountCacheTime)` | Will be removed in SDK version 6. Use `GigyaAccountConfig` instead. |

### `GigyaDefinitions.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `API_GET_SDK_CONFIG` | `public static final String API_GET_SDK_CONFIG` | API endpoint deprecated; IDs now fetched via `socialize.getIDs`. |

### `Gigya.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `getUsedSocialProvider` | `public Provider getUsedSocialProvider(String name)` | Can produce NullPointerException. Use `getUsedSocialProviderWrapper(String name)` instead. |
| `socialLoginWith` | `public void socialLoginWith(List<String> providers, Map<String, Object> params, GigyaLoginCallback<T>)` | No replacement documented. |
| `getAccount` (array overload) | `public void getAccount(String[] include, String[] profileExtraFields, GigyaCallback<T>)` | Use `getAccount(boolean, Map<String, Object>, GigyaCallback<T>)` with `"include"` and `"extraProfileFields"` in the params map. |

### `auth/FidoApiServiceV23Impl.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `FidoApiServiceV23Impl` (class) | `public class FidoApiServiceV23Impl implements IFidoApiService` | Uses the Google FIDO2 API (`play-services-fido`) which Google no longer actively supports. Class retained for compatibility; formal sunset in a future major release. Use `PasskeyAuthenticationProvider` via Credential Manager for new flows. |

### `auth/IWebAuthnService.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `register` (FIDO overload) | `void register(ActivityResultLauncher<IntentSenderRequest>, GigyaCallback<GigyaApiResponse>)` | Use `register(GigyaCallback<GigyaApiResponse>)` (no launcher). |
| `login` (FIDO overload, no params) | `void login(ActivityResultLauncher<IntentSenderRequest>, GigyaLoginCallback<A>)` | Use `login(GigyaLoginCallback<A>)`. |
| `login` (FIDO overload, with params) | `void login(ActivityResultLauncher<IntentSenderRequest>, Map<String, Object>, GigyaLoginCallback<A>)` | Use `login(Map<String, Object>, GigyaLoginCallback<A>)`. |
| `revoke` (no-id overload) | `void revoke(GigyaCallback<GigyaApiResponse>)` | Use `revoke(String id, GigyaCallback<GigyaApiResponse>)`. |
| `handleFidoResult` | `void handleFidoResult(ActivityResult activityResult)` | Part of the retired FIDO2 flow; no replacement. |

### `providers/external/ExternalProvider.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `getProviderSessions` | `public String getProviderSessions(String tokenOrCode, long expiration, String uid)` | Stub returning `null`. To be removed once internal provider classes are cleared. |

### `persistence/PersistenceService.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `savePassKeys` | `public void savePassKeys(String keys)` | Old FIDO2 key storage path. Use `storePasswordLessKey` / `storeMigratedPasswordLessKeys` instead. |
| `getPassKeys` | `public String getPassKeys()` | Use `getPasswordLessKeys()` instead. |
| `clearPassKeys` | `public void clearPassKeys()` | Use `removePasswordLessKey(String id)` instead. |

### `api/BusinessApiService.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `getAccount` (array overload) | `public void getAccount(String[] include, String[] profileExtraFields, GigyaCallback<A>)` | Use `getAccount(Map<String, Object>, GigyaCallback)` with explicit params map. |

### `api/models/GigyaConfigModel.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `GigyaConfigModel` (class) | `public class GigyaConfigModel extends GigyaResponseModel` | Model for deprecated `socialize.getSDKConfig` endpoint. IDs now fetched via `socialize.getIDs`. |

---

## sdk-biometric

### `biometric/GigyaBiometric.java`

| Member | Signature | Reason / Replacement |
|---|---|---|
| `setAnimationForPrePieDevices` | `public void setAnimationForPrePieDevices(boolean animate)` | Animation was only relevant for the legacy pre-Pie fingerprint dialog which has been removed. Now a no-op. Will be removed in a future release. |

---

## Summary

| Module | Count |
|---|---|
| sdk-core | 19 |
| sdk-biometric | 1 |
| **Total** | **20** |
