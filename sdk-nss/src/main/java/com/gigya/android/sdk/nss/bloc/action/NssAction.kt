package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.bloc.flow.INssFlowDelegate
import com.gigya.android.sdk.nss.utils.NssJsonDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

abstract class NssAction<T : GigyaAccount> : INssAction {

    companion object {
        const val submit = "submit"
        const val api = "api"
    }

    var flowDelegate: INssFlowDelegate<*>? = null

    open var gson: Gson = GsonBuilder().registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, NssJsonDeserializer()).create()

    fun flattenArguments(args: MutableMap<String, Any>?) {
        args?.let {
            it.forEach { entry ->
                if (entry.value is Map<*, *>) {
                    args[entry.key] = gson.toJson(entry.value)
                }
            }
        }
    }
}
