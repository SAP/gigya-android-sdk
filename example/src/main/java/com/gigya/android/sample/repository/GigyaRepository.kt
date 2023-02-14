package com.gigya.android.sample.repository

import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.UiThread
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.IAccountService
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.auth.GigyaAuth
import com.gigya.android.sdk.auth.GigyaOTPCallback
import com.gigya.android.sdk.auth.resolvers.IGigyaOtpResult
import com.gigya.android.sdk.interruption.link.ILinkAccountsResolver
import com.gigya.android.sdk.interruption.link.LinkAccountsResolver
import com.gigya.android.sdk.interruption.link.models.ConflictingAccounts
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.session.ISessionService
import com.gigya.android.sdk.tfa.GigyaDefinitions
import com.gigya.android.sdk.tfa.resolvers.email.RegisteredEmailsResolver
import com.gigya.android.sdk.tfa.resolvers.phone.RegisterPhoneResolver
import com.gigya.android.sdk.tfa.resolvers.phone.RegisteredPhonesResolver
import com.gigya.android.sdk.tfa.resolvers.totp.IVerifyTOTPResolver
import com.gigya.android.sdk.tfa.resolvers.totp.RegisterTOTPResolver
import com.gigya.android.sdk.tfa.resolvers.totp.VerifyTOTPResolver
import com.google.gson.Gson
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GigyaRepository {

    companion object {
        const val TAG = "GigyaRepository"
    }

    // Referencing the gigya instance.
    var gigyaInstance: Gigya<MyAccount> = Gigya.getInstance(MyAccount::class.java)
    fun isLoggedIn() = gigyaInstance.isLoggedIn

    var gigyaResolverMap = mutableMapOf<String, Any>()

    fun flushResolvers() = gigyaResolverMap.clear()

    private fun invalidateSession() {
        val accountService = Gigya.getContainer().get(IAccountService::class.java)
        accountService.invalidateAccount()
        val sessionService = Gigya.getContainer().get(ISessionService::class.java)
        sessionService.clear(true)
    }

    fun reinitializeSdk(apiKey: String, dataCenter: String?, cname: String?) {
        invalidateSession()
        if (dataCenter.isNullOrEmpty()) {
            gigyaInstance.init(apiKey)
        } else if (cname != null){
            gigyaInstance.init(apiKey, dataCenter, cname)
        } else {
            gigyaInstance.init(apiKey, dataCenter)
        }
    }

    private fun populateTFARegistrationResolver(
            provider: MutableList<TFAProviderModel>,
            resolverFactory: TFAResolverFactory) {
        provider.forEach { model ->
            when (model.name) {
                GigyaDefinitions.TFAProvider.LIVELINK, GigyaDefinitions.TFAProvider.PHONE -> {
                    gigyaResolverMap["PHONE"] = resolverFactory.getResolverFor(RegisterPhoneResolver::class.java)
                            as RegisterPhoneResolver<MyAccount>
                }
                GigyaDefinitions.TFAProvider.TOTP -> {
                    gigyaResolverMap["TOTP"] = resolverFactory.getResolverFor(RegisterTOTPResolver::class.java)
                            as RegisterTOTPResolver<MyAccount>
                }
            }
        }
    }

    private fun populateTFAVerificationResolver(
            provider: MutableList<TFAProviderModel>,
            resolverFactory: TFAResolverFactory) {
        provider.forEach { model ->
            when (model.name) {
                GigyaDefinitions.TFAProvider.EMAIL -> {
                    gigyaResolverMap["EMAIL"] = resolverFactory.getResolverFor(RegisteredEmailsResolver::class.java)
                            as RegisteredEmailsResolver<MyAccount>
                }
                GigyaDefinitions.TFAProvider.LIVELINK, GigyaDefinitions.TFAProvider.PHONE -> {
                    gigyaResolverMap["PHONE"] = resolverFactory.getResolverFor(RegisteredPhonesResolver::class.java)
                            as RegisteredPhonesResolver<MyAccount>
                }
                GigyaDefinitions.TFAProvider.TOTP -> {
                    gigyaResolverMap["TOTP"] = resolverFactory.getResolverFor(VerifyTOTPResolver::class.java)
                            as VerifyTOTPResolver<MyAccount>
                }
            }
        }
    }

    private fun populateLinkAccountResolver(resolver: ILinkAccountsResolver) {
        gigyaResolverMap["LINK"] = resolver
    }

    private fun loginFlow(initiator: (GigyaLoginCallback<MyAccount>) -> Unit): Flow<GigyaRepoResponse> {
        Log.d(TAG, "loginFlow: initiating")
        val flow: Flow<GigyaRepoResponse> = callbackFlow {
            val callback = object : GigyaLoginCallback<MyAccount>() {
                override fun onSuccess(obj: MyAccount?) {
                    obj?.let {
                        Log.d(TAG, "loginFlow: emmit success")
                        val res = GigyaRepoResponse()
                        flushResolvers()
                        res.account = it
                        trySend(res)
                    }
                }

                override fun onError(error: GigyaError?) {
                    error?.let {
                        Log.d(TAG, "loginFlow: emmit error")
                        val res = GigyaRepoResponse()
                        flushResolvers()
                        res.error = error
                        trySend(res)
                    }
                }

                override fun onConflictingAccounts(response: GigyaApiResponse, resolver: ILinkAccountsResolver) {
                    Log.d(TAG, "loginFlow: emmit Conflicting account interruption")
                    val res = GigyaRepoResponse()
                    res.json = response.asJson()
                    populateLinkAccountResolver(resolver)
                    res.link = LinkInterruption(resolver.conflictingAccounts)
                    trySend(res)
                }

                override fun onPendingTwoFactorRegistration(response: GigyaApiResponse,
                                                            inactiveProviders: MutableList<TFAProviderModel>,
                                                            resolverFactory: TFAResolverFactory) {
                    Log.d(TAG, "loginFlow: emmit TFA registration interruption")
                    val res = GigyaRepoResponse()
                    val tfaInterruption = TFAInterruption(
                            TFAInterruptionType.REGISTRATION,
                            response,
                            inactiveProviders
                    )
                    res.tfa = tfaInterruption
                    populateTFARegistrationResolver(inactiveProviders, resolverFactory)
                    trySend(res)
                }

                override fun onPendingTwoFactorVerification(response: GigyaApiResponse,
                                                            activeProviders: MutableList<TFAProviderModel>,
                                                            resolverFactory: TFAResolverFactory) {
                    Log.d(TAG, "loginFlow: emmit TFA verification interruption")
                    val res = GigyaRepoResponse()
                    val tfaInterruption = TFAInterruption(
                            TFAInterruptionType.VERIFICATION,
                            response,
                            activeProviders
                    )
                    populateTFAVerificationResolver(activeProviders, resolverFactory)
                    res.tfa = tfaInterruption
                    trySend(res)
                }

            }
            initiator(callback)
            awaitClose {
                Log.d(TAG, "loginFlow: close")
            }
        }
        return flow
    }

    fun loginWith(map: MutableMap<String, Any>): Flow<GigyaRepoResponse> {
        return loginFlow { callback ->
            gigyaInstance.login(map, callback)
        }
    }

    fun registerWith(loginID: String, password: String): Flow<GigyaRepoResponse> {
        return loginFlow { callback ->
            gigyaInstance.register(loginID, password, callback)
        }
    }

    fun socialLoginWith(provider: String): Flow<GigyaRepoResponse> {
        return loginFlow { callback ->
            gigyaInstance.login(provider, mutableMapOf(), callback)
        }
    }

    fun ssoLogin(map: MutableMap<String, Any>) : Flow<GigyaRepoResponse> {
        return loginFlow { callback ->
            gigyaInstance.sso(map, callback)
        }
    }

    @UiThread
    suspend fun webAuthnLogin(resultHandler: ActivityResultLauncher<IntentSenderRequest>): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            gigyaInstance.WebAuthn()
                    .login(resultHandler, object : GigyaLoginCallback<MyAccount>() {

                        override fun onSuccess(obj: MyAccount?) {
                            obj?.let {
                                res.account = it
                                continuation.resume(res)
                            }
                        }

                        override fun onError(error: GigyaError?) {
                            error?.let {
                                res.error = error
                                continuation.resume(res)
                            }
                        }

                    })
        }
    }

    @UiThread
    suspend fun webAuthnRegister(resultHandler: ActivityResultLauncher<IntentSenderRequest>): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            gigyaInstance.WebAuthn().register(resultHandler, object : GigyaCallback<GigyaApiResponse>() {
                override fun onSuccess(obj: GigyaApiResponse?) {
                    obj?.let {
                        res.json = obj.asJson()
                        continuation.resume(res)
                    }
                }

                override fun onError(error: GigyaError?) {
                    error?.let {
                        res.error = error
                        continuation.resume(res)
                    }
                }
            })
        }
    }

    @UiThread
    suspend fun webAuthnRevoke(): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            gigyaInstance.WebAuthn().revoke(object : GigyaCallback<GigyaApiResponse>() {
                override fun onSuccess(obj: GigyaApiResponse?) {
                    obj?.let {
                        res.json = obj.asJson()
                        continuation.resume(res)
                    }
                }

                override fun onError(error: GigyaError?) {
                    error?.let {
                        res.error = error
                        continuation.resume(res)
                    }
                }

            })
        }
    }

    fun otpLoginWith(phoneNumber: String): Flow<GigyaRepoResponse> {
        Log.d(TAG, "otpLoginFlow: initiating")
        val flow: Flow<GigyaRepoResponse> = callbackFlow() {
            val callback = object : GigyaOTPCallback<MyAccount>() {
                override fun onSuccess(obj: MyAccount?) {
                    obj?.let {
                        Log.d(TAG, "otpLoginFlow: emmit success")
                        val res = GigyaRepoResponse()
                        flushResolvers()
                        res.account = it
                        trySend(res)
                    }
                }

                override fun onError(error: GigyaError?) {
                    error?.let {
                        Log.d(TAG, "otpLoginFlow: emmit error")
                        val res = GigyaRepoResponse()
                        flushResolvers()
                        res.error = error
                        trySend(res)
                    }
                }

                override fun onPendingOTPVerification(response: GigyaApiResponse,
                                                      resolver: IGigyaOtpResult) {
                    Log.d(TAG, "otpLoginFlow: emmit interruption")
                    gigyaResolverMap["OTP"] = resolver
                    val res = GigyaRepoResponse()
                    res.interruption = "OTP"
                    trySend(res)
                }

            }
            GigyaAuth.getInstance().otp.phoneLogin(phoneNumber, callback)
            awaitClose() {
                Log.d(TAG, "otpLoginFlow: closed")
            }
        }
        return flow
    }

    fun otpVerify(code: String) {
        val resolver = gigyaResolverMap["OTP"]
        resolver?.let {
            (it as IGigyaOtpResult).verify(code)
        }
    }

    @UiThread
    suspend fun registerTfaTotp(): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            val resolver = gigyaResolverMap["TOTP"]
            resolver?.let {
                (resolver as RegisterTOTPResolver<MyAccount>).registerTOTP(object : RegisterTOTPResolver.ResultCallback {
                    override fun onQRCodeAvailable(qrCode: String, verifyTOTPResolver: IVerifyTOTPResolver?) {
                        gigyaResolverMap["TOTP"] = verifyTOTPResolver as VerifyTOTPResolver<MyAccount>
                        res.optional = qrCode
                        continuation.resume(res)
                    }

                    override fun onError(error: GigyaError?) {
                        error?.let {
                            res.error = error
                            continuation.resume(res)
                        }
                    }

                })
            }
        }
    }

    @UiThread
    suspend fun verifyTotpCode(code: String): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            val resolver = gigyaResolverMap["TOTP"]
            resolver?.let {
                (resolver as VerifyTOTPResolver<MyAccount>).verifyTOTPCode(
                        code, true, object : VerifyTOTPResolver.ResultCallback {

                    override fun onResolved() {
                        continuation.resume(res)
                    }

                    override fun onInvalidCode() {
                        res.error = GigyaError(-1, "Invalid code")
                        continuation.resume(res)
                    }

                    override fun onError(error: GigyaError?) {
                        error?.let {
                            res.error = error
                            continuation.resume(res)
                        }
                    }

                }
                )
            }
        }
    }

    @UiThread
    suspend fun logout(): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            gigyaInstance.logout(object : GigyaCallback<GigyaApiResponse>() {
                override fun onSuccess(obj: GigyaApiResponse?) {
                    obj?.let {
                        res.json = obj.asJson()
                        continuation.resume(res)
                    }

                }

                override fun onError(error: GigyaError?) {
                    error?.let {
                        res.error = error
                        continuation.resume(res)
                    }
                }

            })
        }
    }

    @UiThread
    suspend fun addConnection(provider: String): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            gigyaInstance.addConnection(provider, object : GigyaLoginCallback<MyAccount>() {
                override fun onSuccess(obj: MyAccount?) {
                    obj?.let {
                        res.account = it
                        continuation.resume(res)
                    }
                }

                override fun onError(error: GigyaError?) {
                    error?.let {
                        res.error = error
                        continuation.resume(res)
                    }
                }

            })
        }
    }

    @UiThread
    suspend fun removeConnection(provider: String): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            gigyaInstance.removeConnection(provider, object : GigyaCallback<GigyaApiResponse>() {
                override fun onSuccess(obj: GigyaApiResponse?) {
                    obj?.let {
                        res.json = obj.asJson()
                        continuation.resume(res)
                    }
                }

                override fun onError(error: GigyaError?) {
                    error?.let {
                        res.error = error
                        continuation.resume(res)
                    }
                }
            })
        }
    }

    @UiThread
    suspend fun getAccountInfo(): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            gigyaInstance.getAccount(true, object : GigyaCallback<MyAccount>() {
                override fun onSuccess(obj: MyAccount?) {
                    obj?.let {
                        res.account = it
                        continuation.resume(res)
                    }
                }

                override fun onError(error: GigyaError?) {
                    error?.let {
                        res.error = error
                        continuation.resume(res)
                    }
                }

            })
        }

    }

    @UiThread
    suspend fun linkToSite(loginID: String, password: String) {
        return suspendCoroutine {
            val resolver = gigyaResolverMap["LINK"]
            resolver?.let {
                (resolver as LinkAccountsResolver<MyAccount>).linkToSite(loginID, password)
            }
        }
    }

    @UiThread
    suspend fun linkToSocial(provider: String) {
        return suspendCoroutine {
            val resolver = gigyaResolverMap["LINK"]
            resolver?.let {
                (resolver as LinkAccountsResolver<MyAccount>).linkToSocial(provider)
            }
        }
    }
}

open class GigyaRepoResponse {

    var error: GigyaError? = null
    var json: String? = null
    var optional: Any? = null

    // Interruption will be the available resolver tag.
    var interruption: String? = null

    var account: MyAccount? = null
        set(value) {
            field = value
            json = Gson().toJson(value)
        }

    var tfa: TFAInterruption? = null

    var link: LinkInterruption? = null

    fun isError(): Boolean = error != null

    fun isLinkInterruption(): Boolean = link != null

    fun isInterruption(): Boolean = interruption != null

    fun isTfaInterruption(): Boolean = tfa != null
}

data class LinkInterruption(
        var accounts: ConflictingAccounts
)

data class TFAInterruption(
        var type: TFAInterruptionType,
        var originalResponse: GigyaApiResponse,
        var providers: MutableList<TFAProviderModel>)


enum class TFAInterruptionType {
    REGISTRATION,
    VERIFICATION
}
