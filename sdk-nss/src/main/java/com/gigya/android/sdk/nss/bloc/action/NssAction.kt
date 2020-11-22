package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.nss.bloc.data.NssJsEvaluator
import com.gigya.android.sdk.nss.bloc.flow.INssFlowDelegate
import com.gigya.android.sdk.nss.utils.NssJsonDeserializer
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

abstract class NssAction<T : GigyaAccount>(private val businessApi: IBusinessApiService<T>, val jsEvaluator: NssJsEvaluator) : INssAction {

    companion object {
        const val LOG_TAG = "NssAction"

        const val submit = "submit"
        const val api = "api"
        const val socialLogin = "socialLogin"
    }

    var actionId: String? = null

    var flowDelegate: INssFlowDelegate<*>? = null

    open var gson: Gson = GsonBuilder().registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, NssJsonDeserializer()).create()

    override fun onNext(method: String, arguments: MutableMap<String, Any>?) {
        when (method) {
            api -> {
                //TODO Send anonymous api.
            }
            socialLogin -> {
                val provider = arguments?.get("provider") as? String
                provider?.guard {
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
}
