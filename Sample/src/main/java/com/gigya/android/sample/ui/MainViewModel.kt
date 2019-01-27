package com.gigya.android.sample.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.util.Log
import com.gigya.android.sample.GigyaSampleApplication
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaRegisterCallback
import com.gigya.android.sdk.api.RegisterApi
import com.gigya.android.sdk.login.LoginProvider
import com.gigya.android.sdk.login.provider.FacebookLoginProvider
import com.gigya.android.sdk.model.GigyaAccount
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.network.GigyaResponse
import com.google.gson.GsonBuilder

class MainViewModel(application: Application) : AndroidViewModel(application) {

    fun getString(id: Int): String = getApplication<GigyaSampleApplication>().getString(id)

    enum class SetupExample {
        BASIC, CUSTOM_SCHEME
    }

    /*
    Gigya main account scheme model.
     */
    var account: GigyaAccount? = null

    /*
    Custom account scheme model (corresponds with site scheme).
     */
    var myAccount: MyAccount? = null

    /*
    Default setup is basic. Update using activity menu
     */
    var exampleSetup = SetupExample.BASIC

    private val gigya = Gigya.getInstance()

//    private val gigya = Gigya.getInstance(getApplication(), MyAccount::class.java)

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
        when (exampleSetup) {
            SetupExample.BASIC -> {
                gigya.login(loginID, password, object : GigyaCallback<GigyaAccount>() {
                    override fun onSuccess(obj: GigyaAccount?) {
                        success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
                    }

                    override fun onError(error: GigyaError?) {
                        error(error)
                    }
                })
            }
            SetupExample.CUSTOM_SCHEME -> {
//                gigya.login(loginID, password, object : GigyaCallback<MyAccount>() {
//                    override fun onProviderLoginSuccess(obj: MyAccount?) {
//                        success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
//                    }
//
//                    override fun onProviderLoginFailed(error: GigyaError?) {
//                        error(error)
//                    }
//                })
            }
        }
    }

    /**
     * Register using loginID, password, policy.
     */
    fun register(loginID: String, password: String, policy: RegisterApi.RegisterPolicy, success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        flushAccountReferences()
        when (exampleSetup) {
            SetupExample.BASIC -> {
                gigya.register(loginID, password, policy, true, object : GigyaRegisterCallback<GigyaAccount>() {
                    override fun onSuccess(obj: GigyaAccount?) {
                        success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
                    }

                    override fun onError(error: GigyaError?) {
                        error(error)
                    }
                })
            }
            SetupExample.CUSTOM_SCHEME -> {
//                gigya.register(loginID, password, policy, true, object : GigyaRegisterCallback<MyAccount>() {
//                    override fun onProviderLoginSuccess(obj: MyAccount?) {
//                        success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
//                    }
//
//                    override fun onProviderLoginFailed(error: GigyaError?) {
//                        error(error)
//                    }
//                })
            }
        }
    }

    //TODO: Policy.00.0

    /**
     * Get account information.
     */
    fun getAccount(success: (String) -> Unit, error: (GigyaError?) -> Unit) = when (exampleSetup) {
        SetupExample.BASIC -> {
            gigya.getAccount(object : GigyaCallback<GigyaAccount>() {
                override fun onSuccess(obj: GigyaAccount?) {
                    account = obj
                    success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
                }

                override fun onError(error: GigyaError?) {
                    error(error)
                }
            })
        }
        SetupExample.CUSTOM_SCHEME -> {
//            gigya.getAccount(object : GigyaCallback<MyAccount>() {
//                override fun onProviderLoginSuccess(obj: MyAccount?) {
//                    myAccount = obj
//                    success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
//                }
//
//                override fun onProviderLoginFailed(error: GigyaError?) {
//                    error(error)
//                }
//            })
        }
    }

    /**
     * Set account information.
     */
    fun setAccount(dummyData: String, success: (String) -> Unit, error: (GigyaError?) -> Unit) {
        when (exampleSetup) {
            SetupExample.BASIC -> {
                account?.profile?.firstName = dummyData
                gigya.setAccount(account, object : GigyaCallback<GigyaAccount>() {
                    override fun onSuccess(obj: GigyaAccount?) {
                        success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
                    }

                    override fun onError(error: GigyaError?) {
                        error(error)
                    }
                })
            }
            SetupExample.CUSTOM_SCHEME -> {
                myAccount?.data?.report = dummyData
//                gigya.setAccount(myAccount, object : GigyaCallback<GigyaResponse>() {
//                    override fun onProviderLoginSuccess(obj: GigyaResponse?) {
//                        success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
//                    }
//
//                    override fun onProviderLoginFailed(error: GigyaError?) {
//                        error(error)
//                    }
//
//                })
            }
        }
    }

    /**
     * Logout from current session.
     */
    fun logout() {
        flushAccountReferences()
        gigya.logout()
    }

    /**
     * Present SDK native login pre defined UI.
     */
    fun showLoginProviders(success: (String) -> Unit, onIntermediateLoad: () -> Unit, error: (GigyaError?) -> Unit, cancel: () -> Unit) {
        gigya.loginWithSelectedLoginProviders(mutableMapOf<String, Any>(
                "enabledProviders" to "facebook, googlePlus, line, wechat, yahoo"
        ), object : GigyaCallback<GigyaAccount>() {
            override fun onSuccess(obj: GigyaAccount?) {
                Log.d("showLoginProviders", "Success")
                account = obj
                success(GsonBuilder().setPrettyPrinting().create().toJson(obj!!))
            }

            override fun onIntermediateLoad() {
                onIntermediateLoad()
            }

            override fun onCancelledOperation() {
                cancel()
            }

            override fun onError(error: GigyaError?) {
                Log.d("showLoginProviders", "onError")
                error(error)
            }

        })
    }

    /**
     * Request additional Facebook permissions.
     */
    fun requestFacebookPermissionUpdate(granted: () -> Unit, fail: (String) -> Unit, cancel: () -> Unit) {
        val loginProvider: FacebookLoginProvider = gigya.loginProvider as FacebookLoginProvider
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

    fun showScreenSets() {
        val params = mutableMapOf<String, Any>()
        gigya.showPlugin("", params)
    }

    //region Utility methods

    /**
     * Nullify account holders.
     */
    private fun flushAccountReferences() {
        account = null
        myAccount = null
    }

    /**
     * Helper method only.
     * setAccount request is available only when an instance of the account is available (For this tester application only).
     */
    fun okayToRequestSetAccount(): Boolean {
        if (exampleSetup == SetupExample.CUSTOM_SCHEME && myAccount == null) return false
        if (exampleSetup == SetupExample.BASIC && account == null) return false
        return true
    }

    /**
     * Helper method only.
     * Get account name.
     */
    fun getAccountName(): String? {
        return when (exampleSetup) {
            SetupExample.BASIC -> {
                if (account?.profile?.firstName == null) return null
                account?.profile?.firstName + " " + account?.profile?.lastName
            }
            SetupExample.CUSTOM_SCHEME -> {
                if (myAccount?.profile?.firstName == null) return null
                myAccount?.profile?.firstName + " " + myAccount?.profile?.lastName
            }
        }
    }

    /**
     * Helper method only.
     * Get account email address.
     */
    fun getAccountEmail(): String? {
        return when (exampleSetup) {
            SetupExample.BASIC -> account?.profile?.email
            SetupExample.CUSTOM_SCHEME -> account?.profile?.email
        }
    }

    /**
     * Helper method only.
     * Get account profile image URL.
     */
    fun getAccountProfileImage(): String? {
        return when (exampleSetup) {
            SetupExample.BASIC -> account?.profile?.photoURL
            SetupExample.CUSTOM_SCHEME -> account?.profile?.photoURL
        }
    }

    //endregion
}