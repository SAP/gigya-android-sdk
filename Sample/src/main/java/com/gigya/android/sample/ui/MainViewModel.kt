package com.gigya.android.sample.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.support.v4.content.ContextCompat
import android.util.Log
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.GigyaPluginCallback
import com.gigya.android.sdk.api.account.RegisterApi
import com.gigya.android.sdk.interruption.link.LinkAccountsResolver
import com.gigya.android.sdk.interruption.tfa.TFAResolver
import com.gigya.android.sdk.login.LoginProvider
import com.gigya.android.sdk.login.LoginProvider.*
import com.gigya.android.sdk.login.provider.FacebookLoginProvider
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.network.GigyaResponse
import com.gigya.android.sdk.ui.GigyaPresenter
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent
import com.google.gson.GsonBuilder

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // UI event triggering constants.
    companion object {
        const val UI_TRIGGER_SHOW_TFA_REGISTRATION = 1
        const val UI_TRIGGER_SHOW_TFA_VERIFICATION = 2
        const val UI_TRIGGER_SHOW_TFA_CODE_SENT = 3
        const val UI_TRIGGER_SHOW_QR_CODE = 4
        const val UI_TRIGGER_SHOW_CONFLICTING_ACCOUNTS = 5
    }

    /*
     LiveData event trigger.
     */
    val uiTrigger = MutableLiveData<Pair<Int, Any?>>()

    /*
    LiveData fo the custom account scheme model.
     */
    val account = MutableLiveData<MyAccount>()

    /*
    Using short version because SDK was initialized & account scheme was already set
    in the Application class.
     */
    private val gigya = Gigya.getInstance()


    //region TFA

    private var tfaResolver: TFAResolver<*>? = null

    fun onTFAPhoneRegister(phone: String, method: String) {
        tfaResolver?.registerPhone(phone, method)
    }

    fun onTFAPhoneCodeSubmit(code: String) {
        tfaResolver?.submitPhoneCode(code)
    }

    fun onTFAPhoneVerify() {
        tfaResolver?.verifyPhone()
    }

    fun onTFATOTPRegister() {
        tfaResolver?.registerTOTP()
    }

    fun onTFATOTPCodeSubmit(code: String) {
        tfaResolver?.submitTOTPCode(code)
    }

    fun onTFATOTPVerify(code: String) {
        tfaResolver?.verifyTOTP(code)
    }

    fun cancelTFAResolver() {
        tfaResolver?.cancel()
    }

    fun onTFAEmailVerify() {
        tfaResolver?.verifyEmail()
    }

    //endregion

    //region Link accounts

    private var linkAccountsResolver: LinkAccountsResolver<*>? = null

    fun onLinkAccountWithSite(loginID: String, password: String) {
        linkAccountsResolver?.resolveForSiteProvider(loginID, password)
    }

    fun cancelLinkAccountsResolver() {
        linkAccountsResolver?.cancel()
    }

    //endregion

    //region APIS

    /**
     * Send anonymous request.
     */
    fun sendAnonymous(api: String, success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        gigya.send(api, null, object : GigyaCallback<GigyaResponse>() {
            override fun onSuccess(obj: GigyaResponse?) {
                success(obj!!.asJson())
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }
        })
    }

    /**
     * Login using loginID & password.
     */
    fun login(loginID: String, password: String, success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        flushAccountReferences()
        gigya.login(loginID, password, object : GigyaLoginCallback<MyAccount>() {

            override fun onSuccess(obj: MyAccount?) {
                if (tfaResolver != null) {
                    tfaResolver = null
                }
                account.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                if (tfaResolver != null) {
                    tfaResolver = null
                }
                error(error)
            }

            override fun onPendingTFARegistration(response: GigyaResponse, resolver: TFAResolver<*>) {
                tfaResolver = resolver
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_REGISTRATION, resolver.providers))
            }

            override fun onPendingTFAVerification(response: GigyaResponse, resolver: TFAResolver<*>) {
                tfaResolver = resolver
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_VERIFICATION, resolver.providers))
            }

            override fun onPhoneTFAVerificationCodeSent() {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_CODE_SENT, null))
            }

            override fun onTOTPQrCodeAvailable(qrCode: String) {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_QR_CODE, qrCode))
            }
        })
    }

    /**
     * Register using loginID, password, policy.
     */
    fun register(loginID: String, password: String, policy: RegisterApi.RegisterPolicy,
                 success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        flushAccountReferences()
        gigya.register(loginID, password, policy, true, object : GigyaLoginCallback<MyAccount>() {

            override fun onSuccess(obj: MyAccount?) {
                if (tfaResolver != null) {
                    tfaResolver = null
                }
                account.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                if (tfaResolver != null) {
                    tfaResolver = null
                }
                error(error)
            }

            override fun onPendingTFARegistration(response: GigyaResponse, resolver: TFAResolver<*>) {
                tfaResolver = resolver
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_REGISTRATION, resolver.providers))
            }

            override fun onPendingTFAVerification(response: GigyaResponse, resolver: TFAResolver<*>) {
                tfaResolver = resolver
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_VERIFICATION, resolver.providers))
            }

            override fun onPhoneTFAVerificationCodeSent() {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_CODE_SENT, null))
            }

            override fun onTOTPQrCodeAvailable(qrCode: String) {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_QR_CODE, qrCode))
            }
        })
    }

    /**
     * Get account information.
     */
    fun getAccount(success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        gigya.getAccount(object : GigyaCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                account.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }
        })
    }

    /**
     * Set account information.
     */
    fun setAccount(dummyData: String, success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        /* Updating a custom data field. */
        account.value?.data?.comment = dummyData
        gigya.setAccount(account.value, object : GigyaCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                account.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }
        })
    }

    /**
     * Send a reset password email to designated user.
     */
    fun forgotPassword(success: () -> Unit, error: (GigyaError?) -> Unit) {
        gigya.forgotPassword(account.value!!.profile.email, object : GigyaCallback<GigyaResponse>() {
            override fun onSuccess(obj: GigyaResponse?) {
                success()
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }
        })
    }

    /**
     * Logout from current session.
     */
    fun logout() {
        flushAccountReferences()
        gigya.logout()
    }

    /**
     * Login with specific supported social provider.
     */
    fun loginWithProvider(provider: String, success: (String) -> Unit, error: (GigyaError?) -> Unit, cancel: () -> Unit) {
        gigya.login(provider, mapOf<String, Any>(
                GigyaPresenter.PROGRESS_COLOR to ContextCompat.getColor(getApplication(), com.gigya.android.sample.R.color.colorAccent),
                GigyaPresenter.CORNER_RADIUS to 24f), object : GigyaLoginCallback<MyAccount>() {

            override fun onSuccess(obj: MyAccount?) {
                Log.d("loginWithProvider", "Success")
                account.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
                if (linkAccountsResolver != null) {
                    linkAccountsResolver = null
                }
            }

            override fun onError(error: GigyaError?) {
                Log.d("loginWithProvider", "onError")
                if (linkAccountsResolver != null) {
                    linkAccountsResolver = null
                }
                error(error)
            }

            override fun onOperationCancelled() {
                cancel()
            }

            override fun onConflictingAccounts(response: GigyaResponse, resolver: LinkAccountsResolver<*>) {
                // Show custom UI prompting the client that a conflicting account was found.
                linkAccountsResolver = resolver
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_CONFLICTING_ACCOUNTS, resolver.conflictingAccounts))
            }

        })
    }

    //endregion APIS

    /**
     * Present SDK native login pre defined UI.
     */
    fun socialLoginWith(success: (String) -> Unit, onIntermediateLoad: () -> Unit, error: (GigyaError?) -> Unit, cancel: () -> Unit) {
        gigya.socialLoginWith(mutableListOf(FACEBOOK, GOOGLE, LINE, WECHAT, YAHOO), mutableMapOf<String, Any>(
                GigyaPresenter.PROGRESS_COLOR to ContextCompat.getColor(getApplication(), com.gigya.android.sample.R.color.colorAccent),
                GigyaPresenter.CORNER_RADIUS to 24f
        ), object : GigyaLoginCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                Log.d("socialLoginWith", "Success")
                account.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onIntermediateLoad() {
                onIntermediateLoad()
            }

            override fun onOperationCancelled() {
                cancel()
            }

            override fun onError(error: GigyaError?) {
                Log.d("socialLoginWith", "onError")
                error(error)
            }
        })
    }

    /**
     * Request additional Facebook permissions.
     */
    fun requestFacebookPermissionUpdate(granted: () -> Unit, fail: (String) -> Unit, cancel: () -> Unit) {
        val loginProvider: FacebookLoginProvider = gigya.currentProvider as FacebookLoginProvider
        loginProvider.requestPermissionsUpdate(getApplication(), FacebookLoginProvider.READ_PERMISSIONS, listOf("user_birthday"),
                object : LoginProvider.LoginPermissionCallbacks {

                    override fun granted() {
                        Log.d("PermissionUpdate", "granted")
                        granted()
                    }

                    override fun noAccess() {
                        Log.d("PermissionUpdate", "noAccess")
                        failed("No access")
                    }

                    override fun cancelled() {
                        Log.d("PermissionUpdate", "cancelled")
                        cancel()
                    }

                    override fun declined(declined: MutableList<String>?) {
                        Log.d("PermissionUpdate", "declined")
                        fail("Declined")
                    }

                    override fun failed(error: String?) {
                        Log.d("PermissionUpdate", "failed")
                        fail("Error")
                    }

                })
    }

    //TODO Mandatory parameters such as screenSet need to be as a parameter.
    //TODO StringRef for reason.

    fun showAccountDetails(onUpdated: () -> Unit, onCancelled: () -> Unit, onError: (GigyaError?) -> Unit) {
        gigya.showScreenSets(mutableMapOf<String, Any>(
                "screenSet" to "Default-ProfileUpdate",
                GigyaPresenter.SHOW_FULL_SCREEN to true,
                GigyaPresenter.PROGRESS_COLOR to ContextCompat.getColor(getApplication(), com.gigya.android.sample.R.color.colorAccent)),
                object : GigyaPluginCallback<MyAccount>() {
                    override fun onHide(event: GigyaPluginEvent, reason: String?) {
                        if (reason != null) {
                            when (reason) {
                                "finished" -> {
                                    onUpdated()
                                }
                                "canceled" -> {
                                    onCancelled()
                                }
                            }
                        }
                    }

                    override fun onError(event: GigyaPluginEvent) {
                        onError(GigyaError.errorFrom(event.eventMap))
                    }
                })
    }

    fun registrationAsAService(onLogin: (String) -> Unit, onError: (GigyaError?) -> Unit) {
        gigya.showScreenSets(mutableMapOf<String, Any>(
                "screenSet" to "Default-RegistrationLogin",
                GigyaPresenter.PROGRESS_COLOR to ContextCompat.getColor(getApplication(), com.gigya.android.sample.R.color.colorAccent),
                GigyaPresenter.CORNER_RADIUS to 24f
                //, GigyaPresenter.DIALOG_MAX_HEIGHT to 0.8F,
                //GigyaPresenter.DIALOG_MAX_WIDTH to 0.8F
        )
                , object : GigyaPluginCallback<MyAccount>() {
            override fun onLogin(accountObj: MyAccount) {
                account.value = accountObj
                onLogin(GsonBuilder().setPrettyPrinting().create().toJson(accountObj))
            }

            override fun onError(event: GigyaPluginEvent) {
                onError(GigyaError.errorFrom(event.eventMap))
            }

        })
    }

    fun showComments(onLogin: (String) -> Unit, onLogout: () -> Unit, onError: (GigyaError?) -> Unit) {
        gigya.showComments(mutableMapOf<String, Any>(
                "categoryID" to "Support", "streamID" to 1,
                GigyaPresenter.PROGRESS_COLOR to ContextCompat.getColor(getApplication(), com.gigya.android.sample.R.color.colorAccent),
                GigyaPresenter.CORNER_RADIUS to 8f        ),
                object : GigyaPluginCallback<MyAccount>() {

                    override fun onLogin(accountObj: MyAccount) {
                        account.value = accountObj
                        onLogin(GsonBuilder().setPrettyPrinting().create().toJson(accountObj))

                        //TODO This might be wrong - we need to test how to retain the comments activity.
                        showComments(onLogin, onLogout, onError)
                    }

                    override fun onLogout() {
                        onLogout()
                    }
                })
    }

    //region Utility methods

    /**
     * Nullify account holders.
     */
    private fun flushAccountReferences() {
        account.value = null
    }

    /**
     * Helper method only.
     * setAccount request is available only when an instance of the account is available (For this tester application only).
     */
    fun okayToRequestSetAccount(): Boolean {
        if (account.value == null) return false
        return true
    }

    //endregion
}