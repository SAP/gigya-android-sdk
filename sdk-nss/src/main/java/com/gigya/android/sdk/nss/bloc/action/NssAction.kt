package com.gigya.android.sdk.nss.bloc.action

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.auth.IWebAuthnService
import com.gigya.android.sdk.auth.WebAuthnService
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.GigyaNss
import com.gigya.android.sdk.nss.bloc.data.NssJsEvaluator
import com.gigya.android.sdk.nss.bloc.flow.INssFlowDelegate
import com.gigya.android.sdk.nss.utils.NssJsonDeserializer
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import com.gigya.android.sdk.persistence.IPersistenceService
import com.gigya.android.sdk.reporting.ReportingManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.flutter.plugin.common.MethodChannel

abstract class NssAction<T : GigyaAccount>(private val businessApi: IBusinessApiService<T>,
                                           private val jsEvaluator: NssJsEvaluator) : INssAction {

    companion object {
        const val LOG_TAG = "NssAction"

        const val submit = "submit"
        const val api = "api"
        const val socialLogin = "socialLogin"
        const val webAuthnLogin = "webAuthnLogin"
        const val webAuthnRegister = "webAuthnRegister"
        const val webAuthnRevoke = "webAuthnRevoke"
    }

    var actionId: String? = null

    var flowDelegate: INssFlowDelegate<*>? = null

    var webAuthnResultHandler: ActivityResultLauncher<IntentSenderRequest>? = null

    open var gson: Gson = GsonBuilder().registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, NssJsonDeserializer()).create()

    private var globalData = mutableMapOf<String, Any>(
            "Gigya" to mutableListOf("isLoggedIn" to
                    Gigya.getInstance().isLoggedIn, "webAuthnExists" to (getWebAuthnService()?.passKeys?.size!! > 0))
    )

    override fun initialize(expressions: Map<String, String>, result: MethodChannel.Result) {
        doExpressions(globalData, expressions, result)
    }

    fun doExpressions(data: Map<String, Any>, expressions: Map<String, String>, result: MethodChannel.Result) {
        val mergedData = data + globalData
        jsEvaluator.eval(mergedData, expressions) { jsResult ->
            result.success(mapOf("data" to mergedData, "expressions" to jsEvaluator.mapExpressions(jsResult)))
        }
    }

    override fun onNext(method: String, arguments: MutableMap<String, Any>?) {
        when (method) {
            api -> {
                //TODO Send anonymous api.
            }
            webAuthnLogin -> {
                flowDelegate!!.refined<INssFlowDelegate<T>> {
                    getWebAuthnService()?.login(it.getWebAuthnResultHandler(), it.getMainFlowCallback())
                }
            }
            webAuthnRegister -> {
                flowDelegate!!.refined<INssFlowDelegate<T>> {
                    getWebAuthnService()?.register(it.getWebAuthnResultHandler(),
                            object : GigyaCallback<GigyaApiResponse>() {
                                override fun onSuccess(obj: GigyaApiResponse?) {
                                    it.getMainFlowCallback()?.onGenericResponse(obj)
                                }

                                override fun onError(error: GigyaError?) {
                                    it.getMainFlowCallback()?.onError(error)
                                }

                            })
                }
            }
            webAuthnRevoke -> {
                flowDelegate!!.refined<INssFlowDelegate<T>> {
                    getWebAuthnService()?.revoke(
                            object : GigyaCallback<GigyaApiResponse>() {
                                override fun onSuccess(obj: GigyaApiResponse?) {
                                    it.getMainFlowCallback()?.onGenericResponse(obj)
                                }

                                override fun onError(error: GigyaError?) {
                                    it.getMainFlowCallback()?.onError(error)
                                }

                            })
                }
            }
            socialLogin -> {
                val provider = arguments?.get("provider") as? String
                provider?.guard {
                    ReportingManager.get().error(GigyaNss.VERSION, "nss", "Social provider unavailable")
                    GigyaLogger.error(LOG_TAG, "Social provider unavailable")
                }
                flowDelegate!!.refined<INssFlowDelegate<T>> {
                    businessApi.login(provider!!, mutableMapOf(), it.getMainFlowCallback());
                }
            }
        }
    }

    fun flattenArguments(args: MutableMap<String, Any>?) {
        args?.let {
            it.forEach { entry ->
                if (entry.value is Map<*, *>) {
                    args[entry.key] = gson.toJson(entry.value)
                }
            }
        }
    }

    open fun dispose() {
        jsEvaluator.dispose()
    }

    private fun getPersistenceService(): IPersistenceService? {
        try {
            return Gigya.getContainer().get(IPersistenceService::class.java)
        } catch (ex: Exception) {
            ex.printStackTrace()
            GigyaLogger.error(LOG_TAG, "Exception fetching IPersistenceService")
        }
        return null
    }

    private fun getWebAuthnService(): WebAuthnService<T>? {
        try {
            return Gigya.getContainer().get(IWebAuthnService::class.java) as WebAuthnService<T>?
        } catch (ex: Exception) {
            ex.printStackTrace()
            GigyaLogger.error(LOG_TAG, "Exception fetching IWebAuthnService")
        }
        return null
    }
}
