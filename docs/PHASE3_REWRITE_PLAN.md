# Phase 3 — Example App Rewrite Plan

**Status:** ACTIVE — scaffold complete, flow slices pending
**Author:** CDC Mobile (Tal)
**Created:** 2026-08-17
**Branch:** `maintenance-task/p3-scaffold` off `maintenance-feature/phase-3-app-e2e`

---

## 0. Progress Tracker

### Scaffold (p3-scaffold)

| Item | Status |
|---|---|
| Compose BOM + Material3 + Navigation + coroutines in `build.gradle` | ✅ done |
| `linecorp:linesdk` pinned from `latest.release` → `5.8.1` | ✅ done |
| `ui/theme/` — `Color.kt`, `Type.kt`, `Theme.kt` | ✅ done |
| `ui/common/` — `InputField`, `PrimaryButton`, `StatusRow`, `SectionTitle`, `TestTags` | ✅ done |
| `navigation/Screen.kt` + `AppNavGraph.kt` | ✅ done |
| `data/IGigyaRepository` interface | ✅ done |
| `data/GigyaRepository` — correct bridge patterns, no resolver map | ✅ done |
| `data/GigyaSdkException` | ✅ done |
| `data/V5ExternalSessionMigrator` — moved from `repository/`, KDoc updated | ✅ done |
| `repository/` package deleted (empty after migration) | ✅ done |
| `ui/login/LoginViewModel` + `LoginUiState` | ✅ done |
| `ui/login/LoginScreen` + 3 `@Preview`s | ✅ done |
| Stub screens for Account, TFA, Link, OTP, Settings | ✅ done |
| `MainActivity` rewritten as `ComponentActivity` + `NavHost` | ✅ done |
| Old `ui/fragment/` + `MainViewModel` + old `repository/GigyaRepository` deleted | ✅ done |
| XML themes updated to `Theme.AppCompat.DayNight.NoActionBar` | ✅ done |
| `assembleDebug` — BUILD SUCCESSFUL | ✅ done |
| `secrets.xml.template` + `gigyaSdkConfiguration.json.template` | ☐ pending |
| `generateSecrets` Gradle task for CI | ☐ pending |
| Scaffold committed + PR into `maintenance-feature/phase-3-app-e2e` | ☐ pending |

### Flow slices (post-scaffold task branches)

| Task branch | Flow | Status |
|---|---|---|
| `p3-flow-login` | Full LoginScreen (all auth methods + interruptions) | ✅ done — commit `3ab8d768` |
| `p3-flow-register` | Register → session → getAccount | ✅ done — commit `90ec890f` |
| `p3-flow-session` | Session persistence + expiry | ✅ done — commit `d9cbe24d` |
| `p3-flow-biometric` | Biometric opt-in/out/lock/unlock | ✅ done — commit `d3af0166` |
| `p3-flow-tfa` | TFAScreen (TOTP + phone + email) | ✅ done — commit `2a2208c7` |
| `p3-flow-auth` | OIDC / JWT / push-auth | ✅ done — commit `e0cb0a9c` |
| `p3-cross-team-enablement` | One-command E2E run docs | ✅ done — commit `5e1f05ec` |
| `p3-legacy-screens-tail` | Port/drop demo-only screens | ✅ done — commit `cb064af0` |

---

> This document defines the design, scope, and flow parity map for the Compose/MVVM rewrite of the example app. It is the authoritative reference for all Phase 3 task branches. Review and approve before writing any production code.

---

## 1. Goals

1. **Full flow parity** with the current example app — every SDK-exercised flow has a matching screen in the rewrite.
2. **E2E testability** — UI built with explicit `testTag` / `contentDescription` on every interactive element; ViewModel state is observable and deterministic; no hidden side-effects.
3. **Simplicity over aesthetics** — this is a developer test harness, not a customer-facing showcase. Clean, minimal Material3 UI.
4. **No Appium** — tests use Android's built-in instrumentation (`androidx.test`, Espresso, or Compose UI test APIs) against a live test ENV.
5. **Compose-first** — no XML layouts; no Fragments; no manual back-stack management.

---

## 2. Architecture

### 2.1 Stack

| Layer | Choice | Rationale |
|---|---|---|
| UI | Jetpack Compose + Material3 | Modern, testable, stateless by design |
| Navigation | Navigation Compose (`NavHost`) | Replaces manual `FragmentManager`; typed routes; predictable back stack |
| State | `ViewModel` + `StateFlow<UiState>` | One observable state object per screen; survives rotation; E2E-assertable |
| Repository | `GigyaRepository` (singleton) | Single thin wrapper over all SDK calls; no business logic |
| DI | Manual constructor injection (no Hilt) | Keeps the example app simple; Hilt adds scope complexity not needed here |

### 2.3 Repository layer — bridge pattern

`GigyaRepository` is the only place SDK callbacks are handled. The bridge pattern used depends on the callback type — determined by auditing every SDK method used in the example app:

#### One-shot operations → `suspend fun` + `suspendCancellableCoroutine`

Used when the SDK calls back exactly once (success or error). The Compose data layer guide explicitly recommends `suspend fun` for one-shot CRUD operations.

Applies to: `getAccount`, `logout`, `getAuthCode`, `getSaptchaToken`, `addConnection`, `removeConnection`, all `WebAuthn` calls (`login`, `register`, `revoke`, `getCredentials`), resolver follow-up calls (`linkToSite`, `linkToSocial`, `verifyTOTPCode`, `verifyCode`, `registerPhone`, `registerTOTP`).

```kotlin
suspend fun getAccount(): MyAccount = suspendCancellableCoroutine { cont ->
    gigya.getAccount(true, object : GigyaCallback<MyAccount>() {
        override fun onSuccess(obj: MyAccount) { cont.resume(obj) }
        override fun onError(error: GigyaError) { cont.resumeWithException(GigyaSdkException(error)) }
    })
}
```

#### Multi-step / interruption flows → `Flow` + `callbackFlow`

Used when the SDK callback can fire multiple times before reaching a terminal state. The Compose data layer guide recommends `Flow` for data that changes over time.

Applies to:
- `login()`, `register()`, `sso()`, `socialLogin()` — `GigyaLoginCallback` can fire `onPendingTwoFactorRegistration`, `onPendingTwoFactorVerification`, `onConflictingAccounts`, `onCaptchaRequired` before the terminal success/error
- `otp.phoneLogin()` — `GigyaOTPCallback` fires `onPendingOTPVerification` then later success/error after `verify()`

The resolver objects (`ILinkAccountsResolver`, `TFAResolverFactory`, `IGigyaOtpResult`) are held **inside the `callbackFlow` closure** — not in a `gigyaResolverMap`. This eliminates the mutable map entirely; resolvers are released naturally when the flow completes or is cancelled.

```kotlin
fun login(email: String, password: String): Flow<LoginState> = callbackFlow {
    gigya.login(mapOf("loginID" to email, "password" to password),
        object : GigyaLoginCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount) {
                trySend(LoginState.Success(obj)); close()
            }
            override fun onError(error: GigyaError) {
                close(GigyaSdkException(error))
            }
            override fun onPendingTwoFactorRegistration(
                response: GigyaApiResponse, inactiveProviders: List<TFAProviderModel>,
                resolver: TFAResolverFactory
            ) { trySend(LoginState.TFARegistrationRequired(inactiveProviders, resolver)) }

            override fun onPendingTwoFactorVerification(
                response: GigyaApiResponse, activeProviders: List<TFAProviderModel>,
                resolver: TFAResolverFactory
            ) { trySend(LoginState.TFAVerificationRequired(activeProviders, resolver)) }

            override fun onConflictingAccounts(
                response: GigyaApiResponse, resolver: ILinkAccountsResolver
            ) { trySend(LoginState.LinkRequired(resolver)) }

            override fun onCaptchaRequired(captchaVerificationRequired: CaptchaVerificationRequired) {
                trySend(LoginState.CaptchaRequired(captchaVerificationRequired))
            }
        })
    awaitClose { /* no cancel API on SDK — flow closes on terminal state */ }
}
```

#### Session lifecycle → `Flow` + `callbackFlow` + `awaitClose`

The only true register/unregister pair in the SDK. `callbackFlow` with `awaitClose` is the exact pattern Google's docs illustrate for this case.

```kotlin
val sessionState: Flow<SessionEvent> = callbackFlow {
    val observer = SessionStateObserver { trySend(SessionEvent.Expired) }
    gigya.registerSessionExpirationObserver(observer)
    awaitClose { gigya.unregisterSessionExpirationObserver(observer) }
}
```

#### Error handling

- `suspend fun` → throws `GigyaSdkException(error)` — caller uses `try/catch` in ViewModel
- `Flow` → closes with `GigyaSdkException(error)` — caller uses `.catch { }` operator
- `GigyaSdkException` is a thin wrapper around `GigyaError` exposing `errorCode` and `localizedMessage`
- No `Result<T>` wrapper — Google recommends this only when the UI needs to distinguish known error types at the call site; the ViewModel's `UiState.Error` state handles that

```
example/src/main/java/com/gigya/android/sample/
  ├── GigyaSampleApplication.kt        — SDK bootstrap (same as today)
  ├── MainActivity.kt                  — Single activity; NavHost host; FIDO result handler
  ├── data/
  │   ├── GigyaRepository.kt           — All SDK calls; returns Flow / suspend
  │   └── model/
  │       ├── MyAccount.kt             — GigyaAccount subclass
  │       └── MyAccountData.kt         — nested data schema
  ├── ui/
  │   ├── theme/                       — Material3 theme, typography, colors
  │   ├── common/                      — Shared stateless composables
  │   │   ├── InputField.kt
  │   │   ├── PrimaryButton.kt
  │   │   ├── StatusRow.kt             — displays result/error text
  │   │   └── SectionTitle.kt
  │   ├── login/
  │   │   ├── LoginScreen.kt           — stateless composable + @Preview
  │   │   └── LoginViewModel.kt
  │   ├── account/
  │   │   ├── AccountScreen.kt
  │   │   └── AccountViewModel.kt
  │   ├── tfa/
  │   │   ├── TFAScreen.kt
  │   │   └── TFAViewModel.kt
  │   ├── link/
  │   │   ├── LinkAccountScreen.kt
  │   │   └── LinkAccountViewModel.kt
  │   ├── otp/
  │   │   ├── OTPScreen.kt
  │   │   └── OTPViewModel.kt
  │   └── settings/
  │       ├── SettingsScreen.kt
  │       └── SettingsViewModel.kt
  └── navigation/
      └── AppNavGraph.kt               — all routes defined in one place
```

### 2.4 Navigation routes

```kotlin
sealed class Route(val path: String) {
    object Login    : Route("login")
    object Account  : Route("account")
    object TFA      : Route("tfa")
    object Link     : Route("link")
    object OTP      : Route("otp")
    object Settings : Route("settings")
}
```

Interruption flows (TFA, Link) receive their resolver state via the shared `LoginViewModel` which is scoped to the `NavHost` activity — not passed as fragment properties. This eliminates the current fragile pre-commit property-set pattern.

### 2.5 UiState pattern

Every ViewModel exposes a single `sealed interface UiState` following Google's Now in Android convention:

```kotlin
sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val account: MyAccount) : LoginUiState
    data class TFARequired(val interruption: TFAInterruption) : LoginUiState
    data class LinkRequired(val interruption: LinkInterruption) : LoginUiState
    data class Error(val message: String, val errorCode: Int) : LoginUiState
}
```

**State holder — `mutableStateOf` over `StateFlow`:**

Google's current recommendation for Compose-only UIs is `mutableStateOf` — it is simpler and more idiomatic than `StateFlow` since Compose already knows how to observe it directly without `collectAsStateWithLifecycle()`. `StateFlow` is only warranted when state is shared with non-Compose consumers (e.g. a legacy View or another module). Since this is a pure Compose app with no shared observers, `mutableStateOf` is the correct choice:

```kotlin
class LoginViewModel(...) : ViewModel() {
    var uiState by mutableStateOf<LoginUiState>(LoginUiState.Idle)
        private set
}
```

**One-shot events (navigation, errors) are modeled as UiState — not as `Channel` or `SharedFlow`:**

Google explicitly recommends against separate event streams. Navigation triggers (e.g. "go to AccountScreen") and transient messages (errors, snackbars) are represented as states in `UiState`. The screen consumes the state and the ViewModel resets it to `Idle`:

```kotlin
// In ViewModel
fun onLoginSuccess(account: MyAccount) {
    uiState = LoginUiState.Success(account) // screen navigates, then calls onNavigated()
}
fun onNavigated() { uiState = LoginUiState.Idle }
```

**Stability rules:**
- Use `sealed interface` (not `sealed class`) — lighter, no constructor overhead
- Use `data object` for stateless states (Idle, Loading) — Kotlin 1.9+ recommendation
- Use `data class` for states that carry data — all properties must be `val`
- Do not annotate with `@Immutable` or `@Stable` by default — compiler infers stability correctly for `val`-only `data class` / `data object`
- If a state carries a list, use `ImmutableList` from `kotlinx-collections-immutable` so stability is still inferred rather than manually annotated
- `@Immutable` is a last resort only — document exactly why the compiler cannot infer stability

**SDK bridge caveat:**
The Gigya SDK is callback-based. The `GigyaRepository` bridge layer is the only place where non-idiomatic patterns are permitted. ViewModels collect the repository's `Flow` or call `suspend fun` in `viewModelScope.launch`, update `uiState`, and that is all. No SDK callbacks leak above the repository layer.

---

## 3. Flow Parity Map

Every flow in the current app is mapped to its rewrite counterpart. Flows marked **IN SCOPE** are implemented in Phase 3. Flows marked **DEFERRED** are documented but not implemented now.

### 3.1 Authentication flows

| # | Flow | Current implementation | Rewrite screen | Status |
|---|---|---|---|---|
| A1 | Email + password login | `LoginFragment` → `gigya.login()` | `LoginScreen` | IN SCOPE |
| A2 | Email + password register | `LoginFragment` → `gigya.register()` | `LoginScreen` | IN SCOPE |
| A3 | Social login (single provider) | `LoginFragment` text input + `gigya.login(provider)` | `LoginScreen` provider dropdown | IN SCOPE |
| A4 | OTP phone login | `OTPLoginFragment` → `GigyaAuth.otp.phoneLogin()` | `OTPScreen` | IN SCOPE |
| A5 | SSO | `LoginFragment` → `gigya.sso()` | `LoginScreen` SSO button | IN SCOPE |
| A6 | WebAuthn / FIDO passwordless login | `LoginFragment` → `gigya.WebAuthn().login()` | `LoginScreen` passkey button | IN SCOPE |
| A7 | Screen Sets (web) | `LoginFragment` → `gigya.showScreenSet()` | `LoginScreen` button | DEFERRED (demo only) |
| A8 | Native Screen Sets (NSS) | `LoginFragment` → `GigyaNss.load().show()` | `LoginScreen` button | DEFERRED (module sunsetting) |
| A9 | Custom ID login | Implemented in repo/VM but no UI | Not implemented | DEFERRED |

### 3.2 Interruption flows

| # | Flow | Current implementation | Rewrite screen | Status |
|---|---|---|---|---|
| I1 | TFA — TOTP registration | `TFAFragment` QR code path | `TFAScreen` TOTP reg path | IN SCOPE |
| I2 | TFA — TOTP verification | `TFAFragment` code entry | `TFAScreen` code entry | IN SCOPE |
| I3 | TFA — Phone registration | `TFAFragment` phone entry path | `TFAScreen` phone reg path | IN SCOPE |
| I4 | TFA — Phone verification | `TFAFragment` code entry | `TFAScreen` code entry | IN SCOPE |
| I5 | TFA — Email | Resolver wired, no UI | `TFAScreen` email path | IN SCOPE (adds parity gap fix) |
| I6 | Link accounts — site | `LinkAccountFragment` password entry | `LinkAccountScreen` | IN SCOPE |
| I7 | Link accounts — social | `LinkAccountFragment` provider picker | `LinkAccountScreen` | IN SCOPE |
| I8 | Captcha | `LoginFragment` AlertDialog | `LoginScreen` inline state | IN SCOPE |

### 3.3 Post-login / account flows

| # | Flow | Current implementation | Rewrite screen | Status |
|---|---|---|---|---|
| P1 | Get account info | `MyAccountFragment` → `gigya.getAccount()` | `AccountScreen` | IN SCOPE |
| P2 | Logout | `MyAccountFragment` → `gigya.logout()` | `AccountScreen` | IN SCOPE |
| P3 | Add social connection | `MyAccountFragment` → `gigya.addConnection()` | `AccountScreen` | IN SCOPE |
| P4 | Remove social connection | `MyAccountFragment` → `gigya.removeConnection()` | `AccountScreen` | IN SCOPE |
| P5 | Biometric opt-in | `MyAccountFragment` → `biometric.optIn()` | `AccountScreen` biometric section | IN SCOPE |
| P6 | Biometric opt-out | `MyAccountFragment` → `biometric.optOut()` | `AccountScreen` biometric section | IN SCOPE |
| P7 | Biometric lock | `MyAccountFragment` → `biometric.lock()` | `AccountScreen` biometric section | IN SCOPE |
| P8 | Biometric unlock | `LoginFragment`/`MyAccountFragment` → `biometric.unlock()` | `LoginScreen` + `AccountScreen` | IN SCOPE |
| P9 | WebAuthn register passkey | `MyAccountFragment` → `WebAuthn().register()` | `AccountScreen` passkey section | IN SCOPE |
| P10 | WebAuthn revoke passkey | `MyAccountFragment` → `WebAuthn().revoke()` | `AccountScreen` passkey section | IN SCOPE |
| P11 | WebAuthn get credentials | `MyAccountFragment` → `WebAuthn().getCredentials()` | `AccountScreen` passkey section | IN SCOPE |
| P12 | Session expiry handling | `SessionStateObserver` in `MyAccountFragment` | `AccountViewModel` observes session state | IN SCOPE |
| P13 | SDK re-initialize | `SettingsFragment` → `gigya.init()` | `SettingsScreen` | IN SCOPE |
| P14 | SSO session exchange | `SSOExchangeFragment` WebView | `AccountScreen` → opens system browser | DEFERRED (demo only) |
| P15 | Screen Sets (profile update) | `MyAccountFragment` → `gigya.showScreenSet()` | `AccountScreen` button | DEFERRED (demo only) |
| P16 | Native NSS (account update) | `MyAccountFragment` → `GigyaNss.load().show()` | `AccountScreen` button | DEFERRED (module sunsetting) |
| P17 | Push TFA opt-in | Example manifest + `MyAccountFragment` buttons | `AccountScreen` push section | IN SCOPE |
| P18 | Push auth opt-in | Example manifest + `MyAccountFragment` buttons | `AccountScreen` push section | IN SCOPE |

---

## 4. Screen Designs (logical layout)

These are the logical element maps — not pixel designs. Every interactive element must carry a `testTag` matching its element name for test instrumentation.

### 4.1 LoginScreen

```
[ SDK Settings icon (top-right action) ]

─── Credentials ──────────────────────
  [INPUT]  email          testTag: "input_email"
  [INPUT]  password       testTag: "input_password"
  [BTN]    Login          testTag: "btn_login"
  [BTN]    Register       testTag: "btn_register"

─── Passwordless ─────────────────────
  [BTN]    Login with Passkey    testTag: "btn_passkey_login"
  [BTN]    OTP Login             testTag: "btn_otp_login"  → OTPScreen

─── Social ───────────────────────────
  [DROPDOWN] Provider            testTag: "dropdown_social_provider"
  [BTN]    Social Login          testTag: "btn_social_login"

─── SSO ──────────────────────────────
  [BTN]    SSO Login             testTag: "btn_sso"

─── Status ───────────────────────────
  [TEXT]   result / error        testTag: "text_status"
```

### 4.2 AccountScreen

```
─── Account Info ─────────────────────
  [TEXT]   UID display            testTag: "text_uid"
  [BTN]    Get Account            testTag: "btn_get_account"
  [BTN]    Logout                 testTag: "btn_logout"

─── Connections ──────────────────────
  [DROPDOWN] Provider             testTag: "dropdown_connection_provider"
  [BTN]    Add Connection         testTag: "btn_add_connection"
  [BTN]    Remove Connection      testTag: "btn_remove_connection"

─── Passkeys ─────────────────────────
  [BTN]    Register Passkey       testTag: "btn_passkey_register"
  [BTN]    Revoke Passkey         testTag: "btn_passkey_revoke"
  [BTN]    Get Credentials        testTag: "btn_passkey_get"
  [TEXT]   credentials result     testTag: "text_passkey_result"

─── Biometric ────────────────────────
  [TEXT]   biometric status       testTag: "text_biometric_status"
  [BTN]    Opt In / Opt Out       testTag: "btn_biometric_opt"
  [BTN]    Lock / Unlock          testTag: "btn_biometric_lock"

─── Push ─────────────────────────────
  [BTN]    Push TFA Opt-In        testTag: "btn_push_tfa_opt_in"
  [BTN]    Push Auth Opt-In       testTag: "btn_push_auth_opt_in"

─── Status ───────────────────────────
  [TEXT]   result / error         testTag: "text_status"
```

### 4.3 TFAScreen

```
  [TEXT]   "Two-Factor Authentication"
  [DROPDOWN] provider selector    testTag: "dropdown_tfa_provider"

  ── TOTP registration (conditional) ──
  [IMAGE]  QR code display        testTag: "image_qr_code"

  ── Phone registration (conditional) ──
  [INPUT]  phone number           testTag: "input_phone_number"
  [BTN]    Register Phone         testTag: "btn_register_phone"

  ── Email (conditional) ──
  [TEXT]   "Check your email"     testTag: "text_email_tfa_hint"

  ── Verification (shown after registration or on VERIFICATION type) ──
  [INPUT]  verification code      testTag: "input_tfa_code"
  [BTN]    Verify                 testTag: "btn_tfa_verify"

  [TEXT]   result / error         testTag: "text_status"
```

### 4.4 LinkAccountScreen

```
  [TEXT]   "Account conflict — link to continue"
  [DROPDOWN] existing providers   testTag: "dropdown_link_provider"

  ── Site link (conditional on "site" selection) ──
  [INPUT]  password               testTag: "input_link_password"

  [BTN]    Link Account           testTag: "btn_link"
  [TEXT]   result / error         testTag: "text_status"
```

### 4.5 OTPScreen

```
  [TEXT]   "Phone OTP Login"
  [INPUT]  phone number           testTag: "input_otp_phone"
  [BTN]    Send Code              testTag: "btn_otp_send"

  ── Verify (shown after SMS sent) ──
  [INPUT]  verification code      testTag: "input_otp_code"
  [BTN]    Verify                 testTag: "btn_otp_verify"

  [TEXT]   result / error         testTag: "text_status"
```

### 4.6 SettingsScreen

```
  [TEXT]   "Re-initialize SDK"
  [INPUT]  API key                testTag: "input_api_key"
  [INPUT]  data center            testTag: "input_data_center"
  [INPUT]  CNAME                  testTag: "input_cname"
  [BTN]    Apply                  testTag: "btn_reinit"
  [TEXT]   result / error         testTag: "text_status"
```

---

## 5. Testability Requirements

Every screen must satisfy:

1. **All interactive elements have `testTag`** — listed per screen above. Tags are constants in a shared `TestTags.kt` file.
2. **`UiState` is fully observable** — tests can collect `StateFlow` directly without driving UI.
3. **No static SDK calls in composables** — all SDK interaction goes through the ViewModel; composables are pure functions of state.
4. **`@Preview` for every composable** — screens must cover idle, loading, and error states; components cover their variants. See §11.1.
5. **Repository is interface-extracted** — `IGigyaRepository` interface allows fake injection in unit tests.

---

## 6. Social Providers

The rewrite retains the four social provider wrappers (`FacebookProviderWrapper`, `GoogleProviderWrapper`, `LineProviderWrapper`, `WechatProviderWrapper`) unchanged — they are SDK integration code, not example app UI. The `WXEntryActivity` is also retained as-is (WeChat SDK requirement).

Provider names for the dropdown: `facebook`, `google`, `line`, `wechat` (and a free-text "other" option for any Gigya-supported provider).

---

## 7. What is Explicitly NOT Rewritten

| Item | Reason |
|---|---|
| Screen Sets (web) | Demo-only; not part of SDK core test surface |
| Native Screen Sets (NSS) | Module heading toward sunset |
| SSO Exchange WebView | Demo-only |
| Custom ID login | No product requirement to surface it |
| Multi-provider social login (`socialLoginWith(List)`) | Not exposed in current UI either |

These may be added as stub buttons (visible, disabled, labeled "coming soon") so the app layout has room for them without breaking any test flow.

---

## 8. Scaffold Task Scope (p3-scaffold — do first)

The scaffold task delivers the foundation everything else builds on:

- [x] Add Compose BOM + compiler + Material3 to `example/build.gradle`
- [x] Create `ui/theme/` (`Color.kt`, `Type.kt`, `Theme.kt` — minimal Material3, light-only)
- [x] Create `ui/common/` composables (`InputField`, `PrimaryButton`, `StatusRow`, `SectionTitle`) — each with `@Preview`
- [x] Create `navigation/Screen.kt` (sealed class routes) + `AppNavGraph.kt` (all destinations wired, stub screens for non-login routes)
- [x] Create `TestTags.kt` with all tag constants
- [x] Extract `IGigyaRepository` interface; write new `GigyaRepository` in `data/` with correct bridge patterns (`suspendCancellableCoroutine` + `callbackFlow`); eliminate `gigyaResolverMap`
- [x] Wire `MainActivity` to `NavHost` (rewritten as `ComponentActivity`; removed Fragment setup, `viewBinding`, `MainViewModel`)
- [x] Delete old `ui/fragment/` package and `MainViewModel` (replaced by Compose screens + per-screen ViewModels)
- [x] Deliver `LoginScreen` + `LoginViewModel` as reference slice — credentials login + register end-to-end; 3 `@Preview`s (Idle, Loading, Error)
- [x] Fix XML themes to use `Theme.AppCompat.DayNight.NoActionBar` — minimal window wrapper, all theming in `SampleTheme`
- [x] Verified: `./gradlew :example:assembleDebug` — **BUILD SUCCESSFUL**
- [x] Add `secrets.xml.template` + `gigyaSdkConfiguration.json.template` — committed at `example/` root; documents all required keys with placeholders
- [x] Add `generateSecrets` Gradle task — skips locally (no env vars), writes real files from env vars in CI; wired to `preBuild`
- [x] Verified: `./gradlew :example:assembleDebug` — **BUILD SUCCESSFUL**
- [x] Scaffold committed + PR into `maintenance-feature/phase-3-app-e2e` — commit `8fff4963`
- [ ] Add Gradle `generateSecrets` task — reads env vars, writes `secrets.xml` + `gigyaSdkConfiguration.json` at build time (for CI)
- [ ] Verify local `secrets.xml` + `gigyaSdkConfiguration.json` remain gitignored
- [ ] Deliver `LoginScreen` as the one reference slice end-to-end (credentials login + register only; interruptions follow in later tasks)
- [ ] Deliver `LoginViewModel` with `LoginUiState`
- [ ] Confirm `@Preview` renders for `LoginScreen`
- [ ] Confirm build passes

---

## 9. Task Branch Sequence

| Task branch | Deliverable | Depends on |
|---|---|---|
| `p3-scaffold` | Foundation + LoginScreen reference slice | — |
| `p3-flow-login` | Full LoginScreen (all auth methods + interruption wiring) | scaffold |
| `p3-flow-register` | Register → session → getAccount | scaffold |
| `p3-flow-session` | Session persistence + expiry | scaffold |
| `p3-flow-biometric` | Biometric opt-in/out/lock/unlock | scaffold |
| `p3-flow-tfa` | TFAScreen (TOTP + phone + email) | scaffold |
| `p3-flow-auth` | OIDC / JWT / push-auth | scaffold |
| `p3-cross-team-enablement` | One-command E2E run docs | all flows |
| `p3-legacy-screens-tail` | Port/drop demo-only screens | all flows |

---

---

## 11. Coding Standards

These rules apply to every file written in Phase 3 without exception.

### 11.1 Every composable has a `@Preview`

Every `@Composable` function — screens, sections, and shared components — must have a corresponding `@Preview` function in the same file. Previews must cover at least the default/idle state. Complex screens should add previews for loading and error states too.

```kotlin
@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    SampleTheme {
        LoginScreen(uiState = LoginUiState.Idle, ...)
    }
}
```

### 11.2 One responsibility per file

No file should contain more than one screen or more than one logical group of components. Rules:
- One `Screen` composable per file (e.g. `LoginScreen.kt` contains only `LoginScreen` and its direct sub-sections)
- Reusable components live in `ui/common/` — not inlined into screen files
- A file that grows beyond ~150 lines of composable code is a signal to extract a section into its own file
- ViewModels, Screens, and data models are always in separate files — never co-located

### 11.3 KDoc on all public API surfaces

Every `class`, `object`, `interface`, `fun`, and `data class` that is `public` or `internal` must have a KDoc comment. The goal: a developer who forks this repo can understand the purpose of every component without reading the SDK source.

```kotlin
/**
 * Repository abstraction over the Gigya SDK.
 *
 * All SDK interactions flow through this class. It wraps Gigya callbacks
 * into [kotlinx.coroutines.flow.Flow] and suspend functions so ViewModels
 * remain lifecycle-safe and testable.
 */
class GigyaRepository @Inject constructor(...) : IGigyaRepository
```

KDoc must describe *why* and *what*, not just restate the name. One-liners are fine for obvious cases; multi-line for anything non-trivial.

### 11.4 SDK usage must reflect current recommended patterns

The example app is a reference implementation. Every SDK call must use the most current, recommended API:
- No deprecated SDK methods — if a newer API exists, use it
- No workarounds or patches to compensate for SDK limitations — if a limitation exists, document it in a KDoc comment and raise it as a tracked issue instead
- Bridge patterns follow §2.3 exactly — `suspend fun` + `suspendCancellableCoroutine` for one-shot, `callbackFlow` for multi-step/interruption flows and session observers
- Social provider wrappers must use the latest provider SDK versions available at the time of writing
- Any SDK usage pattern that deviates from the documented SDK README must include a KDoc explaining why

**SDK bridge caveat:** The `GigyaRepository` bridge layer is the only permitted non-idiomatic layer — it is a maintenance constraint, not a design flaw. Everything above it (ViewModels, Screens) is idiomatic Kotlin/Compose. No SDK callbacks leak through.

### 11.5 Package structure is enforced

The package layout defined in §2.2 is not a suggestion — it is the required structure. Each flow gets its own sub-package under `ui/`. Shared components always go in `ui/common/`. Navigation is isolated in `navigation/`. No cross-package imports between flow packages (flows communicate only via the shared ViewModel or navigation events, never by importing each other's composables directly).


1. **Push TFA / Push Auth** — both opt-in actions live in `AccountScreen` only (session required). No dedicated screen.

2. **Biometric unlock on launch** — handled via `LaunchedEffect(Unit)` in `LoginScreen` observing a `biometricLockState` exposed by `LoginViewModel`. On locked state, the ViewModel calls `biometric.unlock()` and emits the result into `uiState`. This is idiomatic Compose — no `onResume` override needed.

3. **FIDO `ActivityResultLauncher`** — owned by `MainActivity`, registered before `onStart`. Passed to `LoginViewModel` and `AccountViewModel` via a `setActivityResultCaller(caller)` method called from `MainActivity.onCreate`. No alternative — this is an Android platform constraint.

4. **Secrets / credentials management** — `secrets.xml` stays gitignored for local dev. For GitHub CI and the open-source repo, the solution is:
   - `secrets.xml.template` committed to repo — documents every required key with placeholder values, no real credentials
   - GitHub Actions secrets store the real values — injected at build time via a Gradle `generateSecrets` task that writes `secrets.xml` from env vars
   - `gigyaSdkConfiguration.json` follows the same pattern — a committed `.template` version, real file gitignored, CI generates it from secrets
   - **No API keys ever committed** — the template approach makes the required keys discoverable without leaking values
   - This work is scoped to the scaffold task (§8) since it's foundational for CI in Phase 4

5. **Naming convention** — Compose uses `@Composable` functions. The correct jargon is:
   - `LoginScreen` — the top-level composable that fills the nav destination (what the `NavHost` renders)
   - Inside a screen, smaller composables are called **components** or just composables (e.g. `CredentialsSection`, `BiometricStatusRow`)
   - The term "View" in Compose refers to the legacy Android `View` system — avoid it to prevent confusion
   - Convention used in this project: `<Flow>Screen.kt` for the top-level nav composable, `<Name>Section.kt` or `<Name>Component.kt` for sub-composables within a screen
