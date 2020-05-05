package com.gigya.android.sdk.nss.bloc.flow

import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.interruption.IPendingRegistrationResolver
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.NssEvents
import com.gigya.android.sdk.nss.bloc.action.NssAction
import com.gigya.android.sdk.nss.bloc.action.NssActionFactory
import com.gigya.android.sdk.nss.utils.NssJsonDeserializer
import com.gigya.android.sdk.nss.utils.serializeToMap
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.flutter.plugin.common.MethodChannel

interface INssFlowDelegate<T : GigyaAccount> {

    fun getMainFlowCallback(): GigyaLoginCallback<T>?

    fun getCurrentResult(): MethodChannel.Result?

    fun getResolver(): INssResolver?
}

class NssFlowManager<T : GigyaAccount>(private val actionFactory: NssActionFactory) : INssFlowDelegate<T> {

    private var gson: Gson = GsonBuilder().registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, NssJsonDeserializer()).create()

    private var activeAction: NssAction<*>? = null
    private var activeChannelResult: MethodChannel.Result? = null
    private var activeResolver: INssResolver? = null
    private var mainFlowCallback: GigyaLoginCallback<T>? = null

    var nssEvents: NssEvents<T>? = null

    override fun getMainFlowCallback(): GigyaLoginCallback<T>? = mainFlowCallback

    override fun getCurrentResult(): MethodChannel.Result? = activeChannelResult

    override fun getResolver(): INssResolver? = activeResolver

    init {
        mainFlowCallback = object : GigyaLoginCallback<T>() {

            override fun onSuccess(obj: T) {
                disposeResolver()
                val serializedObject = obj.serializeToMap(gson)
                activeChannelResult?.success(serializedObject)

                // Propagate Nss event.
                nssEvents?.onLogin(obj)
            }

            override fun onError(error: GigyaError?) {
                error?.let { gigyaError ->
                    activeChannelResult?.error(
                            gigyaError.errorCode.toString(),
                            gigyaError.localizedMessage,
                            gigyaError.data
                    )

                    // Propagate Nss error.
                    nssEvents?.onError(gigyaError)
                }
            }

            override fun onPendingRegistration(response: GigyaApiResponse, resolver: IPendingRegistrationResolver) {
                activeResolver = NssResolver(resolver)
                activeChannelResult?.error(response.errorCode.toString(), response.errorDetails, response.asJson())

                // Propagate Nss error. Resolver applied. User should not handle error in host code if applied
                // markup is responsible got interruption.
                nssEvents?.onError(GigyaError.fromResponse(response))
            }

        }
    }

    fun setCurrent(action: String, result: MethodChannel.Result) {
        activeAction = actionFactory.get(action)
        activeAction?.flowDelegate = this
        activeAction?.initialize(result)
    }

    fun onNext(method: String, params: MutableMap<String, Any>, result: MethodChannel.Result) {
        activeChannelResult = result
        activeAction?.onNext(method, params)
    }

    private fun disposeResolver() {
        activeResolver = null;
    }

    fun dispose() {
        // Free current resolver and reset main callback.
        activeResolver = null
        activeAction = null
        activeChannelResult = null
    }
}