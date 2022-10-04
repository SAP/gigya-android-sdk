package com.gigya.android.sample.repository

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.UiThread
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.IAccountService
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.session.ISessionService
import com.google.gson.Gson
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GigyaRepository {

    // Referencing the gigya instance.
    var gigyaInstance: Gigya<MyAccount> = Gigya.getInstance(MyAccount::class.java)
    fun isLoggedIn() = gigyaInstance.isLoggedIn

    private fun invalidateSession() {
        val accountService = Gigya.getContainer().get(IAccountService::class.java)
        accountService.invalidateAccount()
        val sessionService = Gigya.getContainer().get(ISessionService::class.java)
        sessionService.clear(true)
    }

    fun reinitializeSdk(apiKey: String, dataCenter: String?) {
        invalidateSession()
        if (dataCenter.isNullOrEmpty()) {
            gigyaInstance.init(apiKey)
        } else {
            gigyaInstance.init(apiKey, dataCenter)
        }
    }

    @UiThread
    suspend fun login(map: MutableMap<String, Any>): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            gigyaInstance.login(map, object : GigyaLoginCallback<MyAccount>() {
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
    suspend fun register(loginID: String, password: String): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            gigyaInstance.register(loginID, password, object : GigyaLoginCallback<MyAccount>() {
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
    suspend fun socialLoginWithProvider(provider: String): GigyaRepoResponse {
        val res = GigyaRepoResponse()
        return suspendCoroutine { continuation ->
            gigyaInstance.login(
                    provider,
                    mapOf(),
                    object : GigyaLoginCallback<MyAccount>() {
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

}

open class GigyaRepoResponse {

    var error: GigyaError? = null
    var json: String? = null

    var account: MyAccount? = null
        set(value) {
            field = value
            json = Gson().toJson(value)
        }

    fun isError(): Boolean = error != null
}
