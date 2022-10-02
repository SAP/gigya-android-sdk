package com.gigya.android.sample.ui

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sample.repository.GigyaRepository
import com.gigya.android.sdk.GigyaPluginCallback
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val gigyaRepository = GigyaRepository()

    val account: MutableLiveData<MyAccount> by lazy {
        MutableLiveData<MyAccount>()
    }

    fun isLoggedIn() = gigyaRepository.isLoggedIn()

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
    fun showNativeScreenSets() {

    }
}