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
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.GigyaPluginCallback
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.auth.GigyaAuth
import com.gigya.android.sdk.auth.GigyaOTPCallback
import com.gigya.android.sdk.auth.resolvers.IGigyaOtpResult
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.GigyaNss
import com.gigya.android.sdk.nss.NssEvents
import com.gigya.android.sdk.nss.bloc.events.*
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent
import kotlinx.coroutines.launch
import kotlin.coroutines.resume

class MainViewModel : ViewModel() {

    private val gigyaRepository = GigyaRepository()

    val account: MutableLiveData<MyAccount> by lazy {
        MutableLiveData<MyAccount>()
    }

    fun isLoggedIn() = gigyaRepository.isLoggedIn()

    var gigyaResolverMap = mutableMapOf<String, Any>()

    fun reinit(apiKey: String, dataCenter: String?) {
        gigyaRepository.reinitializeSdk(apiKey, dataCenter)
    }

    // Login using email & password pair.
    fun credentialLogin(email: String, password: String,
                        error: (GigyaError?) -> Unit, onLogin: () -> Unit) {
        val params = mutableMapOf<String, Any>("loginID" to email, "password" to password)
        viewModelScope.launch {
            val result = gigyaRepository.login(params)
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            account.value = result.account
            onLogin()
        }
    }

    // Register using email & password pair.
    fun credentialRegister(email: String, password: String,
                           error: (GigyaError?) -> Unit, onLogin: () -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.register(email, password)
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            account.value = result.account
            onLogin()
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

    // Sign in using social login provider.
    fun socialLogin(provider: String, error: (GigyaError?) -> Unit, onLogin: () -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.socialLoginWithProvider(provider)
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            onLogin()
        }
    }

    // Login with Fido passkey (needs to have registered a key before).
    fun passwordlessLogin(resultHandler: ActivityResultLauncher<IntentSenderRequest>,
                          error: (GigyaError?) -> Unit, onLogin: () -> Unit) {
        viewModelScope.launch {
            val result = gigyaRepository.webAuthnLogin(resultHandler)
            if (result.isError()) {
                error(result.error)
                return@launch
            }
            onLogin()
        }
    }

    fun otpLogin(phoneNumber: String,
                 onLogin: () -> Unit,
                 error: (GigyaError?) -> Unit,
                 onPendingOTP: () -> Unit) {
        GigyaAuth.getInstance().otp.phoneLogin(phoneNumber, object : GigyaOTPCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                obj?.let {
                    account.value = it
                    onLogin()
                    // Flush resolver.
                    gigyaResolverMap.remove("OTP")
                }
            }

            override fun onError(error: GigyaError?) {
                error?.let {
                    error(it)
                }
            }

            override fun onPendingOTPVerification(response: GigyaApiResponse, resolver: IGigyaOtpResult) {
                gigyaResolverMap["OTP"] = resolver
                onPendingOTP()
            }

        })
    }

    fun otpVerify(code: String) {
        val resolver = gigyaResolverMap["OTP"]
        resolver?.let {
            (it as IGigyaOtpResult).verify(code)
        }
    }

    // Show web screensets.
    fun showScreenSets(screenset: String,
                       error: (GigyaError?) -> Unit, onLogin: () -> Unit) {
        gigyaRepository.gigyaInstance.showScreenSet(
                screenset,
                true,
                mapOf(),
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
    fun showNativeScreenSets(context: Context, screensetId: String,
                             error: (GigyaError?) -> Unit, onLogin: () -> Unit) {
        GigyaNss.getInstance()
                .load(screensetId)
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
}