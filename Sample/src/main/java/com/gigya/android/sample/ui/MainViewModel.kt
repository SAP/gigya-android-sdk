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
import com.gigya.android.sdk.interruption.ConflictingProviderResolver
import com.gigya.android.sdk.interruption.tfa.TFAResolver
import com.gigya.android.sdk.login.LoginProvider
import com.gigya.android.sdk.login.provider.FacebookLoginProvider
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.network.GigyaResponse
import com.gigya.android.sdk.ui.GigyaPresenter
import com.gigya.android.sdk.ui.plugin.GigyaPluginEvent
import com.google.gson.GsonBuilder

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val UI_TRIGGER_SHOW_TFA_REGISTRATION = 1
        const val UI_TRIGGER_SHOW_TFA_VERIFICATION = 2
    }

    val uiTrigger = MutableLiveData<Pair<Int, Any>>()

    /*
    Custom account scheme model (corresponds with site scheme).
     */
    var myAccount: MyAccount? = null

    /*
    Using short version because SDK was initialized & account scheme was already set
    in the Application class.
     */
    private val gigya = Gigya.getInstance()

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

    private var tfaResolver: TFAResolver<*>? = null

    /**
     * Login using loginID & password.
     */
    fun login(loginID: String, password: String, success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        flushAccountReferences()
        gigya.login(loginID, password, object : GigyaLoginCallback<MyAccount>() {

            override fun onSuccess(obj: MyAccount?) {
                myAccount = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }

            override fun onPendingTFARegistration(resolver: TFAResolver<*>) {
                tfaResolver = resolver
                uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_REGISTRATION, resolver.providers))
            }
        })
    }

    fun onTFAPhoneRegistrationConfirmed(phone: String, method: String) {
        tfaResolver?.phoneResolver?.let { phoneResolver ->
            phoneResolver.register(phone, method)
            uiTrigger.postValue(Pair(UI_TRIGGER_SHOW_TFA_VERIFICATION, tfaResolver!!.providers))
        }
    }

    fun onTFAVerificationConfirmed(provider: String, code: String) {
        tfaResolver?.complete(provider, code)
    }

    //TODO Update register api to simple email, password for sample application.

    /**
     * Register using loginID, password, policy.
     */
    fun register(loginID: String, password: String, policy: RegisterApi.RegisterPolicy,
                 success: (String) -> Unit, error: (GigyaError?) -> Unit, interruption: (Int, Map<String, Any?>) -> Unit) {
        flushAccountReferences()
        gigya.register(loginID, password, policy, true, object : GigyaLoginCallback<MyAccount>() {

            override fun onSuccess(obj: MyAccount?) {
                myAccount = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }
        })
    }

    fun finalizeRegistration(regToken: String, success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        gigya.finalizeRegistration(regToken, object : GigyaLoginCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                myAccount = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onError(error: GigyaError?) {
                error(error)
            }
        })
    }

    /**
     * Get account information.
     */
    fun getAccount(success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        gigya.getAccount(object : GigyaCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                myAccount = obj
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
        myAccount?.profile?.firstName = dummyData
        gigya.setAccount(myAccount, object : GigyaCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
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
        gigya.forgotPassword(myAccount!!.profile.email, object : GigyaCallback<GigyaResponse>() {
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
                GigyaPresenter.CORNER_RADIUS to 24f),
                object : GigyaLoginCallback<MyAccount>() {
                    override fun onSuccess(obj: MyAccount?) {
                        Log.d("loginWithProvider", "Success")
                        myAccount = obj
                        success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
                    }

                    override fun onOperationCancelled() {
                        cancel()
                    }

                    override fun onError(error: GigyaError?) {
                        Log.d("loginWithProvider", "onError")
                        error(error)
                    }

                    override fun onConflictingAccounts(response: GigyaResponse, resolver: ConflictingProviderResolver?) {
                        // Select your provider here from the list using your customized UI.
                        // When done use the resolver to continue the flow.
                        //resolver?.resolveForSiteProvider("toolmarmel.alt2@gmail.com", "123123")
                        //resolver?.resolveForSocialProvider(getApplication(), "googleplus", null)
//                        gigya.showScreenSets(mutableMapOf<String, Any>(
//                                "screenSet" to "Default-LinkAccounts"),
//                                object : GigyaPluginCallback<GigyaAccount>() {
//
//                                }
//                        )
                    }
                })
    }

    //TODO Rename to socialLoginWith. Add List<Providers ? StringRef>..

    /**
     * Present SDK native login pre defined UI.
     */
    fun showLoginProviders(success: (String) -> Unit, onIntermediateLoad: () -> Unit, error: (GigyaError?) -> Unit, cancel: () -> Unit) {
        gigya.loginWithSelectedLoginProviders(mutableMapOf<String, Any>(
                "enabledProviders" to "facebook, googlePlus, line, wechat, yahoo",
                GigyaPresenter.PROGRESS_COLOR to ContextCompat.getColor(getApplication(), com.gigya.android.sample.R.color.colorAccent),
                GigyaPresenter.CORNER_RADIUS to 24f
        ), object : GigyaLoginCallback<MyAccount>() {
            override fun onSuccess(obj: MyAccount?) {
                Log.d("showLoginProviders", "Success")
                myAccount = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onIntermediateLoad() {
                onIntermediateLoad()
            }

            override fun onOperationCancelled() {
                cancel()
            }

            override fun onError(error: GigyaError?) {
                Log.d("showLoginProviders", "onError")
                error(error)
            }

        })
    }

    //TODO Need to create the provider in order to make sure the provider is not null. Add getSocialProvider(String provider).

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
                myAccount = accountObj
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
                GigyaPresenter.CORNER_RADIUS to 8f
        ),
                object : GigyaPluginCallback<MyAccount>() {

                    override fun onLogin(accountObj: MyAccount) {
                        myAccount = accountObj
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
        myAccount = null
    }

    /**
     * Helper method only.
     * setAccount request is available only when an instance of the account is available (For this tester application only).
     */
    fun okayToRequestSetAccount(): Boolean {
        if (myAccount == null) return false
        return true
    }

    /**
     * Helper method only.
     * Get account name.
     */
    fun getAccountName(): String? {
        if (myAccount?.profile?.firstName == null) return null
        return myAccount?.profile?.firstName + " " + myAccount?.profile?.lastName
    }

    /**
     * Helper method only.
     * Get account email address.
     */
    fun getAccountEmail(): String? {
        return myAccount?.profile?.email
    }

    /**
     * Helper method only.
     * Get account profile image URL.
     */
    fun getAccountProfileImage(): String? {
        return myAccount?.profile?.photoURL
    }

    //endregion
}