package com.gigya.android.sample.data

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.fragment.app.FragmentActivity
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.IAccountService
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.auth.GigyaAuth
import com.gigya.android.sdk.auth.GigyaOTPCallback
import com.gigya.android.sdk.biometric.GigyaBiometric
import com.gigya.android.sdk.biometric.GigyaPromptInfo
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback
import com.gigya.android.sdk.biometric.IGigyaBiometricOperationCallback
import com.gigya.android.sdk.interruption.link.ILinkAccountsResolver
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.session.ISessionService
import com.gigya.android.sdk.tfa.GigyaDefinitions.TFAProvider
import com.gigya.android.sdk.tfa.models.EmailModel
import com.gigya.android.sdk.tfa.models.RegisteredPhone
import com.gigya.android.sdk.tfa.resolvers.email.RegisteredEmailsResolver
import com.gigya.android.sdk.tfa.resolvers.IVerifyCodeResolver
import com.gigya.android.sdk.tfa.resolvers.VerifyCodeResolver
import com.gigya.android.sdk.tfa.resolvers.phone.RegisterPhoneResolver
import com.gigya.android.sdk.tfa.resolvers.phone.RegisteredPhonesResolver
import com.gigya.android.sdk.tfa.resolvers.totp.IVerifyTOTPResolver
import com.gigya.android.sdk.tfa.resolvers.totp.RegisterTOTPResolver
import com.gigya.android.sdk.tfa.resolvers.totp.VerifyTOTPResolver
import com.gigya.android.sdk.session.SessionStateObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    private val biometric: GigyaBiometric = GigyaBiometric.getInstance()

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

    /** Credentials registration. Same interruption semantics as [login]. */
    override fun register(email: String, password: String): Flow<LoginState> =
        loginFlow { gigya.register(email, password, mutableMapOf(), it) }

    /**
     * Social login for a single provider (e.g. "facebook", "google").
     * [LoginState.Cancelled] is emitted if the user dismisses the provider UI.
     */
    override fun socialLogin(provider: String): Flow<LoginState> =
        loginFlow { gigya.login(provider, mutableMapOf(), it) }

    /** Mobile SSO login via [Gigya.sso]. */
    override fun ssoLogin(): Flow<LoginState> =
        loginFlow { gigya.sso(mutableMapOf(), it) }

    /**
     * Phone OTP login.
     *
     * Uses `callbackFlow` because [GigyaOTPCallback] fires
     * [LoginState.OTPPending] (with the resolver) before the terminal success
     * or error. The resolver is forwarded in the emitted state so the ViewModel
     * can hold it for the follow-up verify call.
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
        gigya.WebAuthn().login(mutableMapOf<String, Any>(), object : GigyaLoginCallback<MyAccount>() {
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

    // region Biometric

    override val biometricState: BiometricState
        get() = BiometricState(
            isAvailable = biometric.isAvailable(),
            isOptIn = biometric.isOptIn,
            isLocked = biometric.isLocked,
        )

    /**
     * Bridges [GigyaBiometric.optIn] — uses [IGigyaBiometricCallback] which
     * fires once: either [onBiometricOperationSuccess] or [onBiometricOperationFailed].
     * [onBiometricOperationCanceled] is treated as a cancellation.
     */
    override suspend fun biometricOptIn(activity: FragmentActivity): GigyaBiometric.Action =
        suspendCancellableCoroutine { cont ->
            val prompt = GigyaPromptInfo("Biometric Opt-In", "Verify your identity", "")
            biometric.optIn(activity, prompt, object : IGigyaBiometricCallback {
                override fun onBiometricOperationSuccess(action: GigyaBiometric.Action) {
                    cont.resume(action)
                }

                override fun onBiometricOperationFailed(reason: String?) {
                    cont.resumeWithException(Exception(reason ?: "Biometric opt-in failed"))
                }

                override fun onBiometricOperationCanceled() {
                    cont.cancel()
                }
            })
        }

    /**
     * Bridges [GigyaBiometric.optOut] — same one-shot callback pattern as [biometricOptIn].
     */
    override suspend fun biometricOptOut(activity: FragmentActivity): GigyaBiometric.Action =
        suspendCancellableCoroutine { cont ->
            val prompt = GigyaPromptInfo("Biometric Opt-Out", "Verify your identity", "")
            biometric.optOut(activity, prompt, object : IGigyaBiometricCallback {
                override fun onBiometricOperationSuccess(action: GigyaBiometric.Action) {
                    cont.resume(action)
                }

                override fun onBiometricOperationFailed(reason: String?) {
                    cont.resumeWithException(Exception(reason ?: "Biometric opt-out failed"))
                }

                override fun onBiometricOperationCanceled() {
                    cont.cancel()
                }
            })
        }

    /**
     * Bridges [GigyaBiometric.lock] — uses [IGigyaBiometricOperationCallback] (no UI,
     * no cancel path). Lock is immediate and synchronous on the SDK side.
     */
    override suspend fun biometricLock(): GigyaBiometric.Action =
        suspendCancellableCoroutine { cont ->
            biometric.lock(object : IGigyaBiometricOperationCallback {
                override fun onBiometricOperationSuccess(action: GigyaBiometric.Action) {
                    cont.resume(action)
                }

                override fun onBiometricOperationFailed(reason: String?) {
                    cont.resumeWithException(Exception(reason ?: "Biometric lock failed"))
                }
            })
        }

    /**
     * Bridges [GigyaBiometric.unlock] — same one-shot callback pattern as [biometricOptIn].
     */
    override suspend fun biometricUnlock(activity: FragmentActivity): GigyaBiometric.Action =
        suspendCancellableCoroutine { cont ->
            val prompt = GigyaPromptInfo("Unlock Session", "Verify your identity", "")
            biometric.unlock(activity, prompt, object : IGigyaBiometricCallback {
                override fun onBiometricOperationSuccess(action: GigyaBiometric.Action) {
                    cont.resume(action)
                }

                override fun onBiometricOperationFailed(reason: String?) {
                    cont.resumeWithException(Exception(reason ?: "Biometric unlock failed"))
                }

                override fun onBiometricOperationCanceled() {
                    cont.cancel()
                }
            })
        }

    // endregion

    // region Push notifications

    /**
     * Registers for push TFA via [GigyaAuth.registerForAuthPush].
     * The SDK registers the current FCM token with the Gigya backend.
     */
    override suspend fun registerForPushTfa(): String = suspendCancellableCoroutine { cont ->
        GigyaAuth.getInstance().registerForAuthPush(object : GigyaCallback<GigyaApiResponse>() {
            override fun onSuccess(obj: GigyaApiResponse?) {
                cont.resume(obj?.asJson() ?: "")
            }

            override fun onError(error: GigyaError?) {
                error?.let { cont.resumeWithException(GigyaSdkException(it)) }
            }
        })
    }

    /**
     * Registers for push authentication via [GigyaAuth.registerForAuthPush].
     * Push auth and push TFA share the same SDK registration call — the backend
     * differentiates by notification type at delivery time.
     */
    override suspend fun registerForPushAuth(): String = registerForPushTfa()

    // endregion

    // region TFA resolvers

    override suspend fun tfaRegisterPhone(
        resolver: TFAResolverFactory,
        phoneNumber: String,
    ): TFAResolverState = suspendCancellableCoroutine { cont ->
        val phoneResolver = resolver.getResolverFor(RegisterPhoneResolver::class.java)
                as RegisterPhoneResolver<MyAccount>
        phoneResolver.registerPhone(phoneNumber, object : RegisterPhoneResolver.ResultCallback {
            override fun onVerificationCodeSent(verifyCodeResolver: IVerifyCodeResolver?) {
                cont.resume(TFAResolverState.PhoneCodeSent(verifyCodeResolver!!))
            }
            override fun onError(error: GigyaError?) {
                cont.resume(TFAResolverState.Error(error?.localizedMessage ?: "Phone register failed"))
            }
        })
    }

    override suspend fun tfaGetRegisteredPhones(
        resolver: TFAResolverFactory,
    ): TFAResolverState = suspendCancellableCoroutine { cont ->
        val phonesResolver = resolver.getResolverFor(RegisteredPhonesResolver::class.java)
                as RegisteredPhonesResolver<MyAccount>
        phonesResolver.getPhoneNumbers(object : RegisteredPhonesResolver.ResultCallback {
            override fun onRegisteredPhones(phones: MutableList<RegisteredPhone>?) {
                // Automatically sends code to the first registered phone
                cont.resume(TFAResolverState.Resolved)
            }
            override fun onVerificationCodeSent(verifyCodeResolver: IVerifyCodeResolver?) {
                cont.resume(TFAResolverState.PhoneCodeSent(verifyCodeResolver!!))
            }
            override fun onError(error: GigyaError?) {
                cont.resume(TFAResolverState.Error(error?.localizedMessage ?: "Get phones failed"))
            }
        })
    }

    override suspend fun tfaVerifyPhoneCode(
        verifyResolver: IVerifyCodeResolver,
        code: String,
    ): TFAResolverState = suspendCancellableCoroutine { cont ->
        (verifyResolver as VerifyCodeResolver<MyAccount>).verifyCode(
            TFAProvider.PHONE, code, true,
            object : VerifyCodeResolver.ResultCallback {
                override fun onResolved() { cont.resume(TFAResolverState.Resolved) }
                override fun onInvalidCode() { cont.resume(TFAResolverState.InvalidCode) }
                override fun onError(error: GigyaError?) {
                    cont.resume(TFAResolverState.Error(error?.localizedMessage ?: "Verify failed"))
                }
            })
    }

    override suspend fun tfaRegisterTotp(
        resolver: TFAResolverFactory,
    ): TFAResolverState = suspendCancellableCoroutine { cont ->
        val totpResolver = resolver.getResolverFor(RegisterTOTPResolver::class.java)
                as RegisterTOTPResolver<MyAccount>
        totpResolver.registerTOTP(object : RegisterTOTPResolver.ResultCallback {
            override fun onQRCodeAvailable(qrCode: String, verifyTOTPResolver: IVerifyTOTPResolver?) {
                cont.resume(TFAResolverState.QRCodeReady(qrCode, verifyTOTPResolver!!))
            }
            override fun onError(error: GigyaError?) {
                cont.resume(TFAResolverState.Error(error?.localizedMessage ?: "TOTP register failed"))
            }
        })
    }

    override suspend fun tfaVerifyTotpCode(
        verifyResolver: IVerifyTOTPResolver,
        code: String,
    ): TFAResolverState = suspendCancellableCoroutine { cont ->
        (verifyResolver as VerifyTOTPResolver<MyAccount>).verifyTOTPCode(
            code, true,
            object : VerifyTOTPResolver.ResultCallback {
                override fun onResolved() { cont.resume(TFAResolverState.Resolved) }
                override fun onInvalidCode() { cont.resume(TFAResolverState.InvalidCode) }
                override fun onError(error: GigyaError?) {
                    cont.resume(TFAResolverState.Error(error?.localizedMessage ?: "TOTP verify failed"))
                }
            })
    }

    override suspend fun tfaGetRegisteredEmails(
        resolver: TFAResolverFactory,
    ): TFAResolverState = suspendCancellableCoroutine { cont ->
        val emailsResolver = resolver.getResolverFor(RegisteredEmailsResolver::class.java)
                as RegisteredEmailsResolver<MyAccount>
        emailsResolver.getRegisteredEmails(object : RegisteredEmailsResolver.ResultCallback {
            override fun onRegisteredEmails(emails: MutableList<EmailModel>?) {
                cont.resume(TFAResolverState.EmailsLoaded(emails ?: emptyList(), resolver))
            }
            override fun onEmailVerificationCodeSent(verifyCodeResolver: IVerifyCodeResolver?) {
                cont.resume(TFAResolverState.PhoneCodeSent(verifyCodeResolver!!))
            }
            override fun onError(error: GigyaError?) {
                cont.resume(TFAResolverState.Error(error?.localizedMessage ?: "Get emails failed"))
            }
        })
    }

    override suspend fun tfaSendEmailCode(
        resolver: TFAResolverFactory,
        email: EmailModel,
    ): TFAResolverState = suspendCancellableCoroutine { cont ->
        val emailsResolver = resolver.getResolverFor(RegisteredEmailsResolver::class.java)
                as RegisteredEmailsResolver<MyAccount>
        emailsResolver.sendEmailCode(email, object : RegisteredEmailsResolver.ResultCallback {
            override fun onRegisteredEmails(emails: MutableList<EmailModel>?) {
                cont.resume(TFAResolverState.Error("Unexpected emails response"))
            }
            override fun onEmailVerificationCodeSent(verifyCodeResolver: IVerifyCodeResolver?) {
                cont.resume(TFAResolverState.PhoneCodeSent(verifyCodeResolver!!))
            }
            override fun onError(error: GigyaError?) {
                cont.resume(TFAResolverState.Error(error?.localizedMessage ?: "Send email code failed"))
            }
        })
    }

    // endregion

    // region Link account resolver

    override fun linkToSite(
        resolver: ILinkAccountsResolver,
        loginId: String,
        password: String,
    ) = resolver.linkToSite(loginId, password)

    override fun linkToSocial(
        resolver: ILinkAccountsResolver,
        provider: String,
    ) = resolver.linkToSocial(provider)

    // endregion

    // region OTP resolver

    override fun otpVerify(
        resolver: com.gigya.android.sdk.auth.resolvers.IGigyaOtpResult,
        code: String,
    ) = resolver.verify(code)

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

