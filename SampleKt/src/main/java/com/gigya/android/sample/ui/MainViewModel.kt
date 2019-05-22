package com.gigya.android.sample.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.util.Log
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaDefinitions.Plugin.CANCELED
import com.gigya.android.sdk.GigyaDefinitions.Plugin.FINISHED
import com.gigya.android.sdk.GigyaDefinitions.Providers.*
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.GigyaPluginCallback
import com.gigya.android.sdk.interruption.link.IGigyaLinkAccountsResolver
import com.gigya.android.sdk.interruption.tfa.IGigyaTFARegistrationResolver
import com.gigya.android.sdk.interruption.tfa.IGigyaTFAVerificationResolver
import com.gigya.android.sdk.model.tfa.TFAEmail
import com.gigya.android.sdk.model.tfa.TFARegisteredPhone
import com.gigya.android.sdk.network.GigyaApiResponse
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent
import com.google.gson.GsonBuilder

class MainViewModel(application: Application) : AndroidViewModel(application) {

    /*
    UI event triggering constants.
     */
    companion object {
        const val UI_TRIGGER_SHOW_TFA_REGISTRATION = 1
        const val UI_TRIGGER_SHOW_TFA_VERIFICATION = 2
        const val UI_TRIGGER_SHOW_TFA_CODE_SENT = 3
        const val UI_TRIGGER_SHOW_TFA_PHONE_NUMBERS = 4
        const val UI_TRIGGER_SHOW_TFA_EMAILS_AVAILABLE = 5
        const val UI_TRIGGER_SHOW_TFA_EMAIL_SENT = 6
        const val UI_TRIGGER_SHOW_TFA_QR_CODE = 7
        const val UI_TRIGGER_SHOW_CONFLICTING_ACCOUNTS = 8
        const val UI_TRIGGER_DISMISS_ON_ERROR = 9
    }

    override fun onCleared() {
        // Clearing resolvers when the ViewModel clears.
        tfaRegistrationResolver?.clear()
        tfaVerificationResolver?.clear()
        linkAccountsResolver?.clear()
    }

    /*
     LiveData event trigger.
     */
    val uiTrigger = MutableLiveData<Pair<Int, Any?>>()

    /*
    LiveData fo the custom myAccountLiveData scheme model.
     */
    val myAccountLiveData = MutableLiveData<MyAccount>()

    /*
    Using short version because SDK was initialized & myAccountLiveData scheme was already set
    in the Application class.
     */
    private val gigya = Gigya.getInstance(MyAccount::class.java)

    fun isLoggedIn() : Boolean = gigya.isLoggedIn

    //region TFA FLOW

    var tfaRegistrationResolver: IGigyaTFARegistrationResolver? = null

    var tfaVerificationResolver: IGigyaTFAVerificationResolver? = null

    //endregion

    //region LINK ACCOUNTS

    /*
    Keeping reference to the LINK_ACCOUNTS resolver to allow flow continuation.
     */
    private var linkAccountsResolver: IGigyaLinkAccountsResolver? = null

    fun onLinkAccountWithSite(loginID: String, password: String) {
        linkAccountsResolver?.linkToSite(loginID, password)
    }

    fun onLinkAccountWithSocial(provider: String) {
        linkAccountsResolver?.linkToSocial(provider)
    }

    //endregion

    //region APIS

    /**
     * Send anonymous request.
     */
    fun sendAnonymous(api: String, success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        gigya.send(api, null, object : GigyaCallback<GigyaApiResponse>() {
            override fun onSuccess(obj: GigyaApiResponse?) {
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
                myAccountLiveData.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                // Sending a dismiss UI event -> will dismiss TFAFragment if shown.
                uiTrigger.postValue(Pair(UI_TRIGGER_DISMISS_ON_ERROR, null))
                error(error)
            }

            override fun onPendingTFARegistration(response: GigyaApiResponse, resolver: IGigyaTFARegistrationResolver) {
                tfaRegistrationResolver = resolver
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_REGISTRATION, resolver.inactiveProviders))
            }

            override fun onPendingTFAVerification(response: GigyaApiResponse, resolver: IGigyaTFAVerificationResolver) {
                tfaVerificationResolver = resolver
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_VERIFICATION, resolver.activeProviders))
            }

            override fun onRegisteredTFAPhoneNumbers(registeredPhoneList: MutableList<TFARegisteredPhone>) {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_PHONE_NUMBERS, registeredPhoneList))
            }

            override fun onPhoneTFAVerificationCodeSent() {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_CODE_SENT, null))
            }

            override fun onTotpTFAQrCodeAvailable(qrCode: String) {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_QR_CODE, qrCode))
            }

            override fun onEmailTFAAddressesAvailable(emails: MutableList<TFAEmail>?) {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_EMAILS_AVAILABLE, emails))
            }

            override fun onEmailTFAVerificationEmailSent() {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_EMAIL_SENT, null))
            }

        })
    }

    /**
     * Register using loginID, password, policy.
     */
    fun register(loginID: String, password: String, exp: Int,
                 success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        flushAccountReferences()
        gigya.register(loginID, password, mutableMapOf<String, Any>("sessionExpiration" to exp), object : GigyaLoginCallback<MyAccount>() {

            override fun onSuccess(obj: MyAccount?) {
                myAccountLiveData.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                // Sending a dismiss UI event -> will dismiss TFAFragment if shown.
                uiTrigger.postValue(Pair(UI_TRIGGER_DISMISS_ON_ERROR, null))
                error(error)
            }

            override fun onPendingTFARegistration(response: GigyaApiResponse, resolver: IGigyaTFARegistrationResolver) {
                tfaRegistrationResolver = resolver
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_REGISTRATION, resolver.inactiveProviders))
            }

            override fun onRegisteredTFAPhoneNumbers(registeredPhoneList: MutableList<TFARegisteredPhone>) {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_PHONE_NUMBERS, registeredPhoneList))
            }

            override fun onPhoneTFAVerificationCodeSent() {
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_CODE_SENT, null))
            }

        })
    }

    /**
     * Get myAccountLiveData information.
     */
    fun getAccount(success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        gigya.getAccount(object : GigyaCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                myAccountLiveData.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }
        })
    }

    /**
     * Set myAccountLiveData information.
     */
    fun setAccount(dummyData: String, success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        /* Updating a custom data field. */
        when (dummyData.isEmpty()) {
            true -> myAccountLiveData.value?.profile?.email = null
            false -> myAccountLiveData.value?.profile?.email = dummyData
        }
        gigya.setAccount(myAccountLiveData.value, object : GigyaCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                myAccountLiveData.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }
        })
    }

    /**
     * Check login state via accounts.verifyLogin API.
     */
    fun verifyLogin(success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        gigya.verifyLogin(myAccountLiveData.value!!.uid, object : GigyaCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                myAccountLiveData.value = obj
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
        gigya.forgotPassword(myAccountLiveData.value!!.profile?.email, object : GigyaCallback<GigyaApiResponse>() {
            override fun onSuccess(obj: GigyaApiResponse?) {
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
        gigya.login(provider, mutableMapOf<String, Any>(), object : GigyaLoginCallback<MyAccount>() {

            override fun onSuccess(obj: MyAccount?) {
                myAccountLiveData.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }

            override fun onOperationCanceled() {
                cancel()
            }

            override fun onConflictingAccounts(response: GigyaApiResponse, resolver: IGigyaLinkAccountsResolver) {
                linkAccountsResolver = resolver
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_CONFLICTING_ACCOUNTS, resolver.conflictingAccounts))
            }
        })
    }

    //endregion APIS

    //region WEB VIEW

    /**
     * Present SDK native login pre defined UI.
     */
    fun socialLoginWith(success: (String) -> Unit, onIntermediateLoad: () -> Unit, error: (GigyaError?) -> Unit, cancel: () -> Unit) {
        gigya.socialLoginWith(mutableListOf(FACEBOOK, GOOGLE, LINE), mutableMapOf()
                , object : GigyaLoginCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                Log.d("socialLoginWith", "Success")
                myAccountLiveData.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onIntermediateLoad() {
                onIntermediateLoad()
            }

            override fun onOperationCanceled() {
                cancel()
            }

            override fun onError(error: GigyaError?) {
                Log.d("socialLoginWith", "onError")
                error(error)
            }
        })
    }

    fun addConnection(provider: String, success: (String) -> Unit, onIntermediateLoad: () -> Unit, error: (GigyaError?) -> Unit, cancel: () -> Unit) {
        gigya.addConnection(provider, object : GigyaLoginCallback<MyAccount>() {

            override fun onSuccess(obj: MyAccount?) {
                myAccountLiveData.value = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }

            override fun onIntermediateLoad() {
                onIntermediateLoad()
            }

            override fun onOperationCanceled() {
                cancel()
            }
        })
    }

    fun removeConnection(provider: String, success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        gigya.removeConnection(provider, object : GigyaCallback<GigyaApiResponse>() {

            override fun onSuccess(obj: GigyaApiResponse?) {
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }

        })
    }

    /**
     * Display myAccountLiveData details screen set.
     */
    fun showAccountDetails(onUpdated: () -> Unit, onCanceled: () -> Unit, onError: (GigyaError?) -> Unit) {
        gigya.showScreenSet("Default-ProfileUpdate", true, mutableMapOf<String, Any>(),
                object : GigyaPluginCallback<MyAccount>() {
                    override fun onHide(event: GigyaPluginEvent, reason: String?) {
                        if (reason != null) {
                            when (reason) {
                                FINISHED -> {
                                    onUpdated()
                                }
                                CANCELED -> {
                                    onCanceled()
                                }
                            }
                        }
                    }

                    override fun onError(event: GigyaPluginEvent) {
                        onError(GigyaError.errorFrom(event.eventMap))
                    }
                })
    }

    /**
     * Show screen set "Default-RegistrationLogin".
     */
    fun showScreenSets(onLogin: (String) -> Unit, onError: (GigyaError?) -> Unit) {
        gigya.showScreenSet("Default-RegistrationLogin", false, mutableMapOf(),
                object : GigyaPluginCallback<MyAccount>() {
                    override fun onLogin(accountObj: MyAccount) {
                        myAccountLiveData.value = accountObj
                        onLogin(GsonBuilder().setPrettyPrinting().create().toJson(accountObj))
                    }

                    override fun onError(event: GigyaPluginEvent) {
                        onError(GigyaError.errorFrom(event.eventMap))
                    }

                })
    }

    //endregion

    //region UTILITY

    /**
     * Nullify myAccountLiveData holders.
     */
    fun flushAccountReferences() {
        myAccountLiveData.value = null
    }

    /**
     * Helper method only.
     * setAccount request is available only when an instance of the myAccountLiveData is available (For this tester application only).
     */
    fun okayToRequestSetAccount(): Boolean {
        if (myAccountLiveData.value == null) return false
        return true
    }

    //endregion

    fun test() {

    }

}