package com.gigya.android.sample.ui

import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sample.repository.GigyaRepository
import com.gigya.android.sample.repository.LinkInterruption
import com.gigya.android.sample.repository.TFAInterruption
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.GigyaPluginCallback
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.GigyaNss
import com.gigya.android.sdk.nss.NssEvents
import com.gigya.android.sdk.nss.bloc.events.*
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val gigyaRepository = GigyaRepository()

    val account: MutableLiveData<MyAccount> by lazy {
        MutableLiveData<MyAccount>()
    }

    fun isLoggedIn() = gigyaRepository.isLoggedIn()

    fun reinit(apiKey: String, dataCenter: String?, cname: String?) {
        gigyaRepository.reinitializeSdk(apiKey, dataCenter, cname)
    }

    // Login using email & password pair.
    fun credentialLogin(
            email: String, password: String,
            error: (GigyaError?) -> Unit,
            onLogin: () -> Unit,
            tfaInterruption: (TFAInterruption) -> Unit,
            linkInterruption: (LinkInterruption) -> Unit,
    ) {
        val params = mutableMapOf<String, Any>("loginID" to email, "password" to password)
        viewModelScope.launch {
            gigyaRepository.loginWith(params).collect { result ->
                if (result.isError()) {
                    error(result.error)
                    this.coroutineContext.job.cancel()
                } else if (result.isTfaInterruption()) {
                    tfaInterruption(result.tfa!!)
                } else if (result.isLinkInterruption()) {
                    linkInterruption(result.link!!)
                } else {
                    account.value = result.account
                    onLogin()
                    this.coroutineContext.job.cancel()
                }
            }

        }
    }

    // Register using email & password pair.
    fun credentialRegister(email: String, password: String,
                           error: (GigyaError?) -> Unit,
                           params: MutableMap<String, Any>,
                           onLogin: () -> Unit,
                           tfaInterruption: (TFAInterruption) -> Unit,
                           linkInterruption: (LinkInterruption) -> Unit) {
        viewModelScope.launch {
            gigyaRepository.registerWith(email, password, params).collect { result ->
                if (result.isError()) {
                    error(result.error)
                    this.coroutineContext.job.cancel()
                } else if (result.isTfaInterruption()) {
                    tfaInterruption(result.tfa!!)
                } else if (result.isLinkInterruption()) {
                    linkInterruption(result.link!!)
                } else {
                    account.value = result.account
                    onLogin()
                    this.coroutineContext.job.cancel()
                }
            }
        }
    }

    // Request updated account information.
    fun getAccount(error: (GigyaError?) -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.getAccountInfo()
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            account.value = result.account
        }
    }

    // Logout from existing session.
    fun logout(error: (GigyaError?) -> Unit, onLogout: () -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.logout()
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            onLogout()
        }
    }

    fun addConnection(provider: String, error: (GigyaError?) -> Unit, success: () -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.addConnection(provider)
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            success()
        }
    }

    fun removeConnection(provider: String, error: (GigyaError?) -> Unit, success: () -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.removeConnection(provider)
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            success()
        }
    }

    // Sign in using social login provider.
    fun socialLogin(provider: String, error: (GigyaError?) -> Unit, onLogin: () -> Unit, linkInterruption: (LinkInterruption) -> Unit) {
        viewModelScope.launch {
            gigyaRepository.socialLoginWith(provider).collect { result ->
                if (result.isError()) {
                    error(result.error)
                    this.coroutineContext.job.cancel()
                } else if (result.isLinkInterruption()) {
                    linkInterruption(result.link!!)
                } else {
                    account.value = result.account
                    onLogin()
                    this.coroutineContext.job.cancel()
                }
            }
        }
    }

    // Login with Fido passkey (needs to have registered a key before).
    fun passwordlessLogin(sessionExpiration: Int?,
            resultHandler: ActivityResultLauncher<IntentSenderRequest>,
                          error: (GigyaError?) -> Unit, onLogin: () -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.webAuthnLogin(sessionExpiration, resultHandler)
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            onLogin()
        }
    }

    // Register new Fido passkey.
    fun passwordlessRegister(resultHandler: ActivityResultLauncher<IntentSenderRequest>,
                             error: (GigyaError?) -> Unit, success: () -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.webAuthnRegister(resultHandler)
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            success()
        }
    }

    // Revoke current Fido passkey.
    fun passwordlessRevoke(
            error: (GigyaError?) -> Unit, success: () -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.webAuthnRevoke()
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            success()
        }
    }

    // Login using phone OTP.
    fun otpLogin(phoneNumber: String,
                 onLogin: () -> Unit,
                 error: (GigyaError?) -> Unit,
                 onPendingOTP: () -> Unit) {
        viewModelScope.launch {
            gigyaRepository.otpLoginWith(phoneNumber).collect { result ->
                if (result.isError()) {
                    error(result.error)
                    this.coroutineContext.job.cancel()
                } else if (result.isInterruption()) {
                    onPendingOTP()
                } else {
                    account.value = result.account
                    onLogin()
                    this.coroutineContext.job.cancel()
                }
            }
        }
    }

    // Verify phone OTP code.
    fun otpVerify(code: String) {
        gigyaRepository.otpVerify(code)
    }

    // Register TFA authenticator - fetch QR code.
    fun registerTfaTotp(onQrCode: (String) -> Unit, error: (GigyaError?) -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.registerTfaTotp()
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            onQrCode(result.optional as String)
        }
    }

    // Verify TFA authenticator code.
    fun verifyTotpCode(code: String, error: (GigyaError?) -> Unit, onVerified: () -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.verifyTotpCode(code)
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            onVerified()
        }
    }

    // Link account with site credentials.
    fun linkAccountSite(loginId: String, password: String) {
        viewModelScope.launch {
            gigyaRepository.linkToSite(loginId, password)
        }
    }

    // Link account to social provider.
    fun linkAccountSocial(provider: String) {
        viewModelScope.launch {
            gigyaRepository.linkToSocial(provider)
        }
    }

    // Show web screensets.
    fun showScreenSets(screenset: String,
                       error: (GigyaError?) -> Unit, onLogin: () -> Unit) {
        gigyaRepository.gigyaInstance.showScreenSet(
                screenset,
                true,
                mutableMapOf(),
                object : GigyaPluginCallback<MyAccount>() {

                    override fun onLogin(accountObj: MyAccount) {
                        account.value = accountObj
                        onLogin()
                    }

                    override fun onError(event: GigyaPluginEvent?) {
                        event?.let {
                            val eventError = GigyaError.errorFrom(it.eventMap)
                            error(eventError)
                        }
                    }

                    // You can listen to additional events if required.
                }
        )
    }

    // Show native screensets.
    fun showNativeScreenSets(context: Context, screensetId: String, initial: String,
                             error: (GigyaError?) -> Unit, onLogin: () -> Unit, onApiResult: (String, GigyaApiResponse?) -> Unit) {
        GigyaNss.getInstance()
                .load(screensetId)
//                .loadFromAssets("nss-example")
                .initialRoute(initial)
                .events(object : NssEvents<MyAccount>() {

                    override fun onError(screenId: String, error: GigyaError) {
                        error(error)
                    }

                    override fun onCancel() {
                        // Handle cancel event if needed.

                    }

                    override fun onScreenSuccess(screenId: String, action: String, accountObj: MyAccount?) {
                        // Handle login event here if needed.
                        if (accountObj != null) {
                            account.value = accountObj
                            onLogin()
                        }
                    }

                    override fun onApiResult(screenId: String, action: String, api: String, response: GigyaApiResponse?) {
                        onApiResult(api, response)
                    }

                })
                .eventsFor("login", object : NssScreenEvents() {

                    override fun screenDidLoad() {
                        Log.d("NssEvents", "screen did load for login")
                    }

                    override fun routeFrom(screen: ScreenRouteFromModel) {
                        Log.d("NssEvents", "routeFrom: from: " + screen.previousRoute())
                        super.routeFrom(screen)
                    }

                    override fun routeTo(screen: ScreenRouteToModel) {
                        Log.d("NssEvents", "routeTo: to: " + screen.nextRoute() + "data: " + screen.screenData().toString())
                        super.routeTo(screen)
                    }

                    override fun submit(screen: ScreenSubmitModel) {
                        Log.d("NssEvents", "submit: data: " + screen.screenData().toString())
                        super.submit(screen)
                    }

                    override fun fieldDidChange(screen: ScreenFieldModel, field: FieldEventModel) {
                        Log.d("NssEvents", "fieldDidChange: field:" + field.id + " oldVal: " + field.oldVal + " newVal: " + field.newVal)
                        super.fieldDidChange(screen, field)
                    }

                })
                .show(context)
    }

    // Show native screensets.
    fun showNativeScreenSets(context: Context, screensetId: String,
                             error: (GigyaError?) -> Unit, onLogin: () -> Unit) {
        GigyaNss.getInstance()
                .loadFromAssets("nss-example")
                // .load(screensetId)
                .events(object : NssEvents<MyAccount>() {

                    override fun onError(screenId: String, error: GigyaError) {
                        error(error)
                    }

                    override fun onCancel() {
                        // Handle cancel event if needed.

                    }

                    override fun onScreenSuccess(screenId: String, action: String, accountObj: MyAccount?) {
                        // Handle login event here if needed.
                        accountObj?.let {
                            account.value = accountObj
                            onLogin()
                        }
                    }

                })
                .eventsFor("login", object : NssScreenEvents() {

                    override fun screenDidLoad() {
                        Log.d("NssEvents", "screen did load for login")
                    }

                    override fun routeFrom(screen: ScreenRouteFromModel) {
                        Log.d("NssEvents", "routeFrom: from: " + screen.previousRoute())
                        super.routeFrom(screen)
                    }

                    override fun routeTo(screen: ScreenRouteToModel) {
                        Log.d("NssEvents", "routeTo: to: " + screen.nextRoute() + "data: " + screen.screenData().toString())
                        super.routeTo(screen)
                    }

                    override fun submit(screen: ScreenSubmitModel) {
                        Log.d("NssEvents", "submit: data: " + screen.screenData().toString())
                        super.submit(screen)
                    }

                    override fun fieldDidChange(screen: ScreenFieldModel, field: FieldEventModel) {
                        Log.d("NssEvents", "fieldDidChange: field:" + field.id + " oldVal: " + field.oldVal + " newVal: " + field.newVal)
                        super.fieldDidChange(screen, field)
                    }

                })
                .show(context)
    }

    // Mobile sso authentication.
    fun mobileSSO(
            error: (GigyaError?) -> Unit,
            canceled: () -> Unit,
            onLogin: () -> Unit,
    ) {
        viewModelScope.launch {
            gigyaRepository.ssoLogin(mutableMapOf("rp_context" to mutableMapOf(
                    "contextKey" to "contextValue"
            )

            )).collect { result ->
                if (result.isCanceled()) {
                    canceled()
                    this.coroutineContext.job.cancel()
                }
                else if (result.isError()) {
                    error(result.error)
                    this.coroutineContext.job.cancel()
                } else {
                    account.value = result.account
                    onLogin()
                    this.coroutineContext.job.cancel()
                }
            }

        }
    }
}