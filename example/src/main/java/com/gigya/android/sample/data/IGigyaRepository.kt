package com.gigya.android.sample.data

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.api.GigyaApiResponse
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

    // region SDK re-initialisation

    /**
     * Re-initialises the SDK with a new API key / data center.
     * Clears the current session and account cache before re-init.
     */
    fun reinitializeSdk(apiKey: String, dataCenter: String?, cname: String?)

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
