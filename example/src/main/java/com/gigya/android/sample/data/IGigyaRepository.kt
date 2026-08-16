package com.gigya.android.sample.data

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.fragment.app.FragmentActivity
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.biometric.GigyaBiometric
import com.gigya.android.sdk.interruption.link.models.ConflictingAccounts
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel
import com.gigya.android.sdk.network.GigyaError
import kotlinx.coroutines.flow.Flow

/**
 * Contract for all Gigya SDK interactions.
 *
 * Abstracting the repository behind this interface allows ViewModels to be
 * tested in isolation using a fake implementation, without requiring a real
 * SDK instance or network connectivity.
 *
 * Bridge pattern used per §2.3 of PHASE3_REWRITE_PLAN.md:
 * - `suspend fun` for one-shot SDK callbacks ([GigyaCallback]).
 * - [Flow] + `callbackFlow` for multi-step flows ([GigyaLoginCallback],
 *   [GigyaOTPCallback]) that can emit interruption states before terminal success/error.
 * - [sessionState] uses `callbackFlow` + `awaitClose` for the register/unregister pair.
 */
interface IGigyaRepository {

    /** True if a valid session is currently active. */
    val isLoggedIn: Boolean

    /**
     * Emits [SessionEvent]s for the lifetime of the collector.
     * Automatically unregisters the session observer when collection ends.
     */
    val sessionState: Flow<SessionEvent>

    // region Authentication flows — emit [LoginState] via callbackFlow

    /** Credentials login. May emit [LoginState.TFARequired], [LoginState.LinkRequired],
     *  or [LoginState.CaptchaRequired] before the terminal [LoginState.Success] or [LoginState.Error]. */
    fun login(email: String, password: String): Flow<LoginState>

    /** Credentials registration. Same interruption semantics as [login]. */
    fun register(email: String, password: String): Flow<LoginState>

    /** Social login for a single provider (e.g. "facebook", "google"). */
    fun socialLogin(provider: String): Flow<LoginState>

    /** Mobile SSO login. */
    fun ssoLogin(): Flow<LoginState>

    /** Phone OTP login. Emits [LoginState.OTPPending] when the SMS has been sent. */
    fun otpLogin(phoneNumber: String): Flow<LoginState>

    // endregion

    // region One-shot operations — suspend fun

    /** Fetches the current account. Force-refreshes from server. */
    suspend fun getAccount(): MyAccount

    /** Logs out the current session. */
    suspend fun logout()

    /** Fetches an SSO auth code for session exchange. */
    suspend fun getAuthCode(): String

    /** Fetches a saptcha token for captcha-protected requests. */
    suspend fun getSaptchaToken(): String

    /** Adds a social connection to the current account. */
    suspend fun addConnection(provider: String): MyAccount

    /** Removes a social connection from the current account. */
    suspend fun removeConnection(provider: String)

    // endregion

    // region WebAuthn / Passkeys

    /**
     * Initiates a WebAuthn passkey login.
     * Requires an [ActivityResultLauncher] registered in [MainActivity] before `onStart`.
     */
    suspend fun webAuthnLogin(
        resultHandler: ActivityResultLauncher<IntentSenderRequest>,
    ): MyAccount

    /** Registers a new WebAuthn passkey for the current account. */
    suspend fun webAuthnRegister(
        resultHandler: ActivityResultLauncher<IntentSenderRequest>,
    )

    /** Revokes all WebAuthn passkeys for the current account. */
    suspend fun webAuthnRevoke()

    /** Returns the list of registered WebAuthn credentials as raw JSON. */
    suspend fun webAuthnGetCredentials(): String

    // endregion

    // region Biometric

    /**
     * Current biometric state snapshot.
     * Query this before rendering the biometric section of [AccountScreen].
     */
    val biometricState: BiometricState

    /**
     * Opts the current session into biometric protection.
     * Requires a [FragmentActivity] for the BiometricPrompt system UI.
     */
    suspend fun biometricOptIn(activity: FragmentActivity): GigyaBiometric.Action

    /**
     * Opts the current session out of biometric protection.
     * Session must be unlocked before calling.
     */
    suspend fun biometricOptOut(activity: FragmentActivity): GigyaBiometric.Action

    /**
     * Locks the current session. No UI required — lock is immediate.
     * The user must call [biometricUnlock] to resume the session.
     */
    suspend fun biometricLock(): GigyaBiometric.Action

    /**
     * Unlocks the current session using the device biometric.
     * Requires a [FragmentActivity] for the BiometricPrompt system UI.
     */
    suspend fun biometricUnlock(activity: FragmentActivity): GigyaBiometric.Action

    // endregion

    // region Push notifications

    /**
     * Registers the device for push TFA notifications.
     * Requires the FCM token to be available (firebase-messaging initialised).
     */
    suspend fun registerForPushTfa(): String

    /**
     * Registers the device for push authentication notifications.
     */
    suspend fun registerForPushAuth(): String

    // endregion

    // region SDK re-initialisation

    /**
     * Re-initialises the SDK with a new API key / data center.
     * Clears the current session and account cache before re-init.
     */
    fun reinitializeSdk(apiKey: String, dataCenter: String?, cname: String?)

    // endregion

    // region TFA resolvers

    /**
     * Registers a phone number for TFA. Calls [RegisterPhoneResolver.registerPhone].
     * On success returns a [IVerifyCodeResolver] wrapped in [TFAResolverState.PhoneCodeSent].
     */
    suspend fun tfaRegisterPhone(
        resolver: TFAResolverFactory,
        phoneNumber: String,
    ): TFAResolverState

    /**
     * Gets registered phone numbers for TFA verification.
     * Calls [RegisteredPhonesResolver.getPhoneNumbers].
     */
    suspend fun tfaGetRegisteredPhones(
        resolver: TFAResolverFactory,
    ): TFAResolverState

    /**
     * Verifies a phone TFA code. Calls [VerifyCodeResolver.verifyCode].
     */
    suspend fun tfaVerifyPhoneCode(
        verifyResolver: com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver,
        code: String,
    ): TFAResolverState

    /**
     * Registers a TOTP authenticator. Calls [RegisterTOTPResolver.registerTOTP].
     * On success returns [TFAResolverState.QRCodeReady] with the QR code string
     * and a [IVerifyTOTPResolver] for the follow-up verify step.
     */
    suspend fun tfaRegisterTotp(resolver: TFAResolverFactory): TFAResolverState

    /**
     * Verifies a TOTP code. Calls [VerifyTOTPResolver.verifyTOTPCode].
     */
    suspend fun tfaVerifyTotpCode(
        verifyResolver: com.gigya.android.sdk.tfa.resolvers.totp.IVerifyTOTPResolver,
        code: String,
    ): TFAResolverState

    // endregion

    // region Link account resolver

    /**
     * Links the conflicting account to an existing site account.
     * Calls [LinkAccountsResolver.linkToSite]. The original login flow
     * resumes automatically via the [GigyaLoginCallback] held by the resolver.
     */
    fun linkToSite(
        resolver: com.gigya.android.sdk.interruption.link.ILinkAccountsResolver,
        loginId: String,
        password: String,
    )

    /**
     * Links the conflicting account to an existing social account.
     * Calls [LinkAccountsResolver.linkToSocial].
     */
    fun linkToSocial(
        resolver: com.gigya.android.sdk.interruption.link.ILinkAccountsResolver,
        provider: String,
    )

    // endregion

    // region OTP resolver

    /**
     * Verifies the OTP code sent via [otpLogin].
     * Calls [IGigyaOtpResult.verify] — the original [GigyaOTPCallback] then
     * fires success or error back into the [otpLogin] flow.
     */
    fun otpVerify(
        resolver: com.gigya.android.sdk.auth.resolvers.IGigyaOtpResult,
        code: String,
    )

    // endregion
}

// ---------------------------------------------------------------------------
// State models emitted by Flow-based repository methods
// ---------------------------------------------------------------------------

/**
 * Represents every possible state emitted during a login or registration flow.
 *
 * The flow emits intermediate interruption states before the terminal
 * [Success] or [Error]. ViewModels map these to their own [LoginUiState].
 */
sealed interface LoginState {
    /** Terminal: authentication succeeded. */
    data class Success(val account: MyAccount) : LoginState

    /** Terminal: SDK returned an error. */
    data class Error(val error: GigyaError) : LoginState

    /** Terminal: user dismissed the social/SSO provider UI. */
    data object Cancelled : LoginState

    /** Interruption: TFA registration required — user must set up an authenticator. */
    data class TFARegistrationRequired(
        val providers: List<TFAProviderModel>,
        val resolver: TFAResolverFactory,
    ) : LoginState

    /** Interruption: TFA verification required — user must enter a code. */
    data class TFAVerificationRequired(
        val providers: List<TFAProviderModel>,
        val resolver: TFAResolverFactory,
    ) : LoginState

    /** Interruption: conflicting account — user must link to an existing account. */
    data class LinkRequired(
        val accounts: ConflictingAccounts,
        val resolver: com.gigya.android.sdk.interruption.link.ILinkAccountsResolver,
    ) : LoginState

    /** Interruption: captcha verification required before login can proceed. */
    data class CaptchaRequired(val response: GigyaApiResponse) : LoginState

    /** Interruption: OTP SMS sent — user must enter the verification code. */
    data class OTPPending(
        val resolver: com.gigya.android.sdk.auth.resolvers.IGigyaOtpResult,
    ) : LoginState
}

/**
 * Events emitted by [IGigyaRepository.sessionState].
 */
sealed interface SessionEvent {
    /** The active session has expired. */
    data object Expired : SessionEvent
}

/**
 * Snapshot of the current biometric opt-in / lock state.
 *

 * Queried synchronously from [GigyaBiometric] — no SDK callback required.
 *
 * @property isAvailable Whether the device supports biometric authentication.
 * @property isOptIn Whether the current session is enrolled for biometric protection.
 * @property isLocked Whether the session is currently locked by biometric.
 */
data class BiometricState(
    val isAvailable: Boolean,
    val isOptIn: Boolean,
    val isLocked: Boolean,
)

/**
 * Result states emitted by TFA resolver operations.
 *
 * Each TFA step is a one-shot suspend call that returns one of these states.
 * The ViewModel maps these to [TFAUiState] for the screen to render.
 */
sealed interface TFAResolverState {
    /** Phone code sent — holds the resolver for the follow-up verify step. */
    data class PhoneCodeSent(
        val verifyResolver: com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver,
    ) : TFAResolverState

    /** TOTP QR code available — holds the QR string and verify resolver. */
    data class QRCodeReady(
        val qrCode: String,
        val verifyResolver: com.gigya.android.sdk.tfa.resolvers.totp.IVerifyTOTPResolver,
    ) : TFAResolverState

    /** TFA step resolved — the original login flow will complete via its callback. */
    data object Resolved : TFAResolverState

    /** TFA code was invalid — user should retry. */
    data object InvalidCode : TFAResolverState

    /** An error occurred. */
    data class Error(val message: String) : TFAResolverState
}
