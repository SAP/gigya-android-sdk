package com.gigya.android.sample.data

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.IAccountService
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.auth.GigyaAuth
import com.gigya.android.sdk.auth.GigyaOTPCallback
import com.gigya.android.sdk.interruption.link.ILinkAccountsResolver
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.session.ISessionService
import com.gigya.android.sdk.session.SessionStateObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Production implementation of [IGigyaRepository].
 *
 * All Gigya SDK callbacks are bridged here — nothing above this class
 * (ViewModels, Screens) is aware of SDK callback types.
 *
 * Bridge rules (per §2.3 of PHASE3_REWRITE_PLAN.md):
 * - [GigyaCallback] (one-shot) → `suspendCancellableCoroutine`
 * - [GigyaLoginCallback] / [GigyaOTPCallback] (multi-step) → `callbackFlow`
 * - Session observer (register/unregister pair) → `callbackFlow` + `awaitClose`
 *
 * Resolver objects are held inside the `callbackFlow` closure — no mutable
 * resolver map is needed and resolvers are released when the flow closes.
 */
class GigyaRepository : IGigyaRepository {

    private val gigya: Gigya<MyAccount> = Gigya.getInstance(MyAccount::class.java)

    override val isLoggedIn: Boolean
        get() = gigya.isLoggedIn

    // region Session state

    /**
     * Emits [SessionEvent.Expired] when the SDK fires a session-expiry notification.
     * Uses `callbackFlow` + `awaitClose` — the only true register/unregister pair
     * in the Gigya SDK public API.
     */
    override val sessionState: Flow<SessionEvent> = callbackFlow {
        val observer = SessionStateObserver { trySend(SessionEvent.Expired) }
        gigya.registerSessionExpirationObserver(observer)
        awaitClose { gigya.unregisterSessionExpirationObserver(observer) }
    }

    // endregion

    // region Authentication flows

    /**
     * Credentials login.
     *
     * Uses `callbackFlow` because [GigyaLoginCallback] may fire multiple
     * interruption overrides ([LoginState.TFARegistrationRequired],
     * [LoginState.TFAVerificationRequired], [LoginState.LinkRequired],
     * [LoginState.CaptchaRequired]) before the terminal success or error.
     * Resolver objects are captured in the callback closure and forwarded
     * directly in the emitted state — no external resolver map needed.
     */
    override fun login(email: String, password: String): Flow<LoginState> =
        loginFlow { gigya.login(mutableMapOf<String, Any>("loginID" to email, "password" to password), it) }

    /**
     * Credentials registration. Same interruption semantics as [login].
     */
    override fun register(email: String, password: String): Flow<LoginState> =
        loginFlow { gigya.register(email, password, mutableMapOf(), it) }

    /**
     * Social login for a single provider (e.g. "facebook", "google").
     * [LoginState.Cancelled] is emitted if the user dismisses the provider UI.
     */
    override fun socialLogin(provider: String): Flow<LoginState> =
        loginFlow { gigya.login(provider, mutableMapOf(), it) }

    /**
     * Mobile SSO login via [Gigya.sso].
     */
    override fun ssoLogin(): Flow<LoginState> =
        loginFlow { gigya.sso(mutableMapOf(), it) }

    /**
     * Phone OTP login.
     *
     * Uses `callbackFlow` because [GigyaOTPCallback] fires
     * [LoginState.OTPPending] (with the resolver) before the terminal success
     * or error. The resolver is forwarded in the emitted state so the ViewModel
     * can hold it for the follow-up [verify] call.
     */
    override fun otpLogin(phoneNumber: String): Flow<LoginState> = callbackFlow {
        GigyaAuth.getInstance().otp.phoneLogin(
            phoneNumber,
            object : GigyaOTPCallback<MyAccount>() {
                override fun onSuccess(obj: MyAccount?) {
                    obj?.let { trySend(LoginState.Success(it)); close() }
                }

                override fun onError(error: GigyaError?) {
                    error?.let { close(GigyaSdkException(it)) }
                }

                override fun onPendingOTPVerification(
                    response: GigyaApiResponse,
                    resolver: com.gigya.android.sdk.auth.resolvers.IGigyaOtpResult,
                ) {
                    trySend(LoginState.OTPPending(resolver))
                }
            },
        )
        // No cancel API on the SDK — the flow closes on the terminal callback.
        awaitClose { Log.d(TAG, "otpLogin flow closed") }
    }

    // endregion

    // region One-shot operations

    override suspend fun getAccount(): MyAccount = suspendCancellableCoroutine { cont ->
        gigya.getAccount(true, object : GigyaCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                obj?.let { cont.resume(it) }
            }

            override fun onError(error: GigyaError?) {
                error?.let { cont.resumeWithException(GigyaSdkException(it)) }
            }
        })
    }

    override suspend fun logout(): Unit = suspendCancellableCoroutine { cont ->
        gigya.logout(object : GigyaCallback<GigyaApiResponse>() {
            override fun onSuccess(obj: GigyaApiResponse?) { cont.resume(Unit) }
            override fun onError(error: GigyaError?) {
                error?.let { cont.resumeWithException(GigyaSdkException(it)) }
            }
        })
    }

    override suspend fun getAuthCode(): String = suspendCancellableCoroutine { cont ->
        gigya.getAuthCode(object : GigyaCallback<String>() {
            override fun onSuccess(code: String?) {
                code?.let { cont.resume(it) }
            }

            override fun onError(error: GigyaError?) {
                error?.let { cont.resumeWithException(GigyaSdkException(it)) }
            }
        })
    }

    override suspend fun getSaptchaToken(): String = suspendCancellableCoroutine { cont ->
        gigya.getSaptchaToken(object : GigyaCallback<GigyaApiResponse>() {
            override fun onSuccess(obj: GigyaApiResponse?) {
                val token = obj?.getField("saptchaToken", String::class.java) ?: ""
                cont.resume(token)
            }

            override fun onError(error: GigyaError?) {
                error?.let { cont.resumeWithException(GigyaSdkException(it)) }
            }
        })
    }

    override suspend fun addConnection(provider: String): MyAccount =
        suspendCancellableCoroutine { cont ->
            gigya.addConnection(provider, object : GigyaLoginCallback<MyAccount>() {
                override fun onSuccess(obj: MyAccount?) {
                    obj?.let { cont.resume(it) }
                }

                override fun onError(error: GigyaError?) {
                    error?.let { cont.resumeWithException(GigyaSdkException(it)) }
                }
            })
        }

    override suspend fun removeConnection(provider: String): Unit =
        suspendCancellableCoroutine { cont ->
            gigya.removeConnection(provider, object : GigyaCallback<GigyaApiResponse>() {
                override fun onSuccess(obj: GigyaApiResponse?) { cont.resume(Unit) }
                override fun onError(error: GigyaError?) {
                    error?.let { cont.resumeWithException(GigyaSdkException(it)) }
                }
            })
        }

    // endregion

    // region WebAuthn / Passkeys

    override suspend fun webAuthnLogin(
        resultHandler: ActivityResultLauncher<IntentSenderRequest>,
    ): MyAccount = suspendCancellableCoroutine { cont ->
        val params = mutableMapOf<String, Any>()
        gigya.WebAuthn().login(params, object : GigyaLoginCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                obj?.let { cont.resume(it) }
            }

            override fun onError(error: GigyaError?) {
                error?.let { cont.resumeWithException(GigyaSdkException(it)) }
            }
        })
    }

    override suspend fun webAuthnRegister(
        resultHandler: ActivityResultLauncher<IntentSenderRequest>,
    ): Unit = suspendCancellableCoroutine { cont ->
        gigya.WebAuthn().register(object : GigyaCallback<GigyaApiResponse>() {
            override fun onSuccess(obj: GigyaApiResponse?) { cont.resume(Unit) }
            override fun onError(error: GigyaError?) {
                error?.let { cont.resumeWithException(GigyaSdkException(it)) }
            }
        })
    }

    override suspend fun webAuthnRevoke(): Unit = suspendCancellableCoroutine { cont ->
        gigya.WebAuthn().revoke(object : GigyaCallback<GigyaApiResponse>() {
            override fun onSuccess(obj: GigyaApiResponse?) { cont.resume(Unit) }
            override fun onError(error: GigyaError?) {
                error?.let { cont.resumeWithException(GigyaSdkException(it)) }
            }
        })
    }

    override suspend fun webAuthnGetCredentials(): String = suspendCancellableCoroutine { cont ->
        gigya.WebAuthn().getCredentials(object : GigyaCallback<GigyaApiResponse>() {
            override fun onSuccess(obj: GigyaApiResponse?) {
                cont.resume(obj?.asJson() ?: "")
            }

            override fun onError(error: GigyaError?) {
                error?.let { cont.resumeWithException(GigyaSdkException(it)) }
            }
        })
    }

    // endregion

    // region SDK re-initialisation

    override fun reinitializeSdk(apiKey: String, dataCenter: String?, cname: String?) {
        Gigya.getContainer().get(IAccountService::class.java).invalidateAccount()
        Gigya.getContainer().get(ISessionService::class.java).clear(true)
        when {
            dataCenter.isNullOrEmpty() -> gigya.init(apiKey)
            cname != null -> gigya.init(apiKey, dataCenter, cname)
            else -> gigya.init(apiKey, dataCenter)
        }
    }

    // endregion

    // region Private helpers

    /**
     * Shared `callbackFlow` factory for all [GigyaLoginCallback]-based flows.
     *
     * [GigyaLoginCallback] is not a simple one-shot callback — it can fire
     * multiple interruption overrides before the terminal success or error.
     * `callbackFlow` is the correct bridge for this multi-emission pattern.
     * Resolver objects are captured in the lambda and forwarded in the emitted
     * [LoginState], so no external map is needed.
     *
     * @param initiator Lambda that calls the SDK method with the provided callback.
     */
    private fun loginFlow(
        initiator: (GigyaLoginCallback<MyAccount>) -> Unit,
    ): Flow<LoginState> = callbackFlow {
        initiator(object : GigyaLoginCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                obj?.let { trySend(LoginState.Success(it)); close() }
            }

            override fun onError(error: GigyaError?) {
                error?.let { close(GigyaSdkException(it)) }
            }

            override fun onOperationCanceled() {
                trySend(LoginState.Cancelled); close()
            }

            override fun onConflictingAccounts(
                response: GigyaApiResponse,
                resolver: ILinkAccountsResolver,
            ) {
                trySend(LoginState.LinkRequired(resolver.conflictingAccounts, resolver))
            }

            override fun onPendingTwoFactorRegistration(
                response: GigyaApiResponse,
                inactiveProviders: MutableList<TFAProviderModel>,
                resolverFactory: TFAResolverFactory,
            ) {
                trySend(LoginState.TFARegistrationRequired(inactiveProviders, resolverFactory))
            }

            override fun onPendingTwoFactorVerification(
                response: GigyaApiResponse,
                activeProviders: MutableList<TFAProviderModel>,
                resolverFactory: TFAResolverFactory,
            ) {
                trySend(LoginState.TFAVerificationRequired(activeProviders, resolverFactory))
            }

            override fun onCaptchaRequired(response: GigyaApiResponse) {
                trySend(LoginState.CaptchaRequired(response))
            }
        })
        // The SDK has no cancel API — the flow closes on the terminal callback.
        awaitClose { Log.d(TAG, "loginFlow closed") }
    }

    // endregion

    companion object {
        private const val TAG = "GigyaRepository"
    }
}

/**
 * Wraps a [GigyaError] as a [Throwable] so it can be used with
 * `suspendCancellableCoroutine`'s `resumeWithException` and caught in
 * ViewModel `try/catch` blocks like any other exception.
 *
 * @property error The original SDK error.
 */
class GigyaSdkException(val error: GigyaError) : Exception(error.localizedMessage)
