package com.gigya.android.sdk.nss.bloc.flow

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.interruption.IPendingRegistrationResolver
import com.gigya.android.sdk.interruption.link.ILinkAccountsResolver
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.NssEvents
import com.gigya.android.sdk.nss.bloc.GigyaNssCallback
import com.gigya.android.sdk.nss.bloc.action.NssAction
import com.gigya.android.sdk.nss.bloc.action.NssActionFactory
import com.gigya.android.sdk.nss.utils.NssJsonDeserializer
import com.gigya.android.sdk.nss.utils.serializeToMap
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.flutter.plugin.common.MethodChannel
import org.json.JSONObject

interface INssFlowDelegate<T : GigyaAccount> {

    fun getMainFlowCallback(): GigyaNssCallback<T, GigyaApiResponse>?

    fun getCurrentResult(): MethodChannel.Result?

    fun getResolver(): INssResolver?

    fun getActiveScreen(): String?

    fun getWebAuthnResultHandler(): ActivityResultLauncher<IntentSenderRequest>?

    fun getGson(): Gson
}

class NssFlowManager<T : GigyaAccount>(private val actionFactory: NssActionFactory) :
    INssFlowDelegate<T> {

    private var gson: Gson = GsonBuilder().registerTypeAdapter(
        object : TypeToken<Map<String?, Any?>?>() {}.type,
        NssJsonDeserializer()
    ).create()

    override fun getGson() = gson

    private var activeScreen: String? = null
    var activeAction: NssAction<*>? = null
    private var activeChannelResult: MethodChannel.Result? = null
    private var activeResolver: INssResolver? = null
    private var mainFlowCallback: GigyaNssCallback<T, GigyaApiResponse>? = null
    var fidoResultHandler: ActivityResultLauncher<IntentSenderRequest>? = null

    var nssEvents: NssEvents<T>? = null

    override fun getMainFlowCallback(): GigyaNssCallback<T, GigyaApiResponse>? = mainFlowCallback

    override fun getCurrentResult(): MethodChannel.Result? = activeChannelResult

    override fun getResolver(): INssResolver? = activeResolver

    override fun getWebAuthnResultHandler(): ActivityResultLauncher<IntentSenderRequest>? =
        fidoResultHandler

    init {
        mainFlowCallback = object : GigyaNssCallback<T, GigyaApiResponse>() {

            override fun onGenericResponse(res: GigyaApiResponse?, api: String?) {
                res.let {
                    val serializedObject = it.serializeToMap(gson)

                    // Merge data with updated global data.
                    val merged =
                        (serializedObject["mapped"] as Map<out String, Any>) + activeAction!!.getGlobalData()

                    // Return merged result to engine.
                    activeChannelResult?.success(merged)

                    if (api != null) {
                        nssEvents?.onApiResult(
                            activeScreen!!,
                            activeAction!!.actionId!!,
                            api,
                            res
                        )
                        return@let
                    }
                    // Propagate Nss event.
                    nssEvents?.onScreenSuccess(
                        activeScreen!!,
                        activeAction!!.actionId!!,
                        null
                    )
                }
            }

            override fun onSuccess(obj: T) {
                disposeResolver()
                val serializedObject = obj.serializeToMap(gson)
                activeChannelResult?.success(serializedObject)

                // Propagate Nss event.
                nssEvents?.onScreenSuccess(
                    activeScreen!!,
                    activeAction!!.actionId!!,
                    obj
                )
            }

            override fun onOperationCanceled() {
                // Send operation canceled event.
                activeChannelResult?.error(
                    "200001", // Operation canceled error.
                    "error-operation-canceled",
                    "{ }"
                )
            }

            override fun onError(error: GigyaError?) {
                error?.let { gigyaError ->
                    val json = JSONObject(gigyaError.data)
                    activeChannelResult?.error(
                        gigyaError.errorCode.toString(),
                        when(json.optString("errorMessage")) {
                            "" -> gigyaError.localizedMessage
                            else -> json.getString("errorMessage")
                        },
                        gigyaError.data
                    )

                    // Propagate Nss error.
                    val errObject = GigyaError(
                        gigyaError.data,
                        gigyaError.errorCode,
                        when(json.optString("errorMessage")) {
                            "" -> gigyaError.localizedMessage
                            else -> json.getString("errorMessage")
                        },
                        gigyaError.callId
                    )
                    nssEvents?.onError(
                        activeScreen!!,
                        errObject
                    )
                }
            }

            override fun onPendingRegistration(
                response: GigyaApiResponse,
                resolver: IPendingRegistrationResolver
            ) {
                activeResolver = NssResolver(resolver)
                activeChannelResult?.error(
                    response.errorCode.toString(),
                    response.getField("errorMessage", String::class.java)?.toString(),
                    response.asJson()
                )

                // Propagate Nss error. Resolver applied. User should not handle error in host code if applied.
                // Markup is responsible for interruption. handling
                val error = GigyaError(
                    response.asJson(),
                    response.errorCode,
                    response.getField("errorMessage", String::class.java)?.toString(),
                    response.callId
                )
                nssEvents?.onError(
                    activeScreen!!,
                    error
                )
            }

            override fun onPendingVerification(response: GigyaApiResponse, regToken: String?) {
                activeChannelResult?.error(
                    response.errorCode.toString(),
                    response.getField("errorMessage", String::class.java)?.toString(),
                    response.asJson()
                )

                // Propagate Nss error. No resolver available for specific interruption.
                // Markup is responsible for interruption handling although this specific error breaks any flow.
                // onPendingVerification == pending email verification error.
                val error = GigyaError(
                    response.asJson(),
                    response.errorCode,
                    response.getField("errorMessage", String::class.java)?.toString(),
                    response.callId
                )
                nssEvents?.onError(
                    activeScreen!!,
                    error
                )
            }

            override fun onConflictingAccounts(
                response: GigyaApiResponse,
                resolver: ILinkAccountsResolver
            ) {
                activeResolver = NssResolver(resolver)

                activeChannelResult?.error(
                    response.errorCode.toString(),
                    response.getField("errorMessage", String::class.java)?.toString(),
                    response.asJson()
                )

                // Propagate Nss error. Resolver applied. User should not handle error in host code if applied.
                // Markup is responsible for interruption. handling
                val error = GigyaError(
                    response.asJson(),
                    response.errorCode,
                    response.getField("errorMessage", String::class.java)?.toString(),
                    response.callId
                )
                nssEvents?.onError(
                    activeScreen!!,
                    error
                )
            }

        }
    }

    /**
     * Update the current action.
     */
    fun setCurrent(
        action: String,
        screenId: String,
        expressions: Map<String, String>,
        result: MethodChannel.Result
    ) {
        if (activeAction?.actionId == action) {
            // Do not create a new instance of the same action.
            activeAction!!.initialize(expressions, result)
            return
        }
        activeScreen = screenId
        activeAction?.dispose()
        activeAction = actionFactory.get(action)
        activeAction?.flowDelegate = this
        activeAction?.initialize(expressions, result)
    }

    /**
     * Handle next action request.
     */
    fun onNext(method: String, params: MutableMap<String, Any>, result: MethodChannel.Result?) {
        activeChannelResult = result
        activeAction?.onNext(method, params)
    }

    /**
     * Nullify the current active resolver.
     * Only one resolver is active per flow.
     */
    private fun disposeResolver() {
        activeResolver = null;
    }

    fun dispose() {
        // Free current resolver and reset main callback.
        activeResolver = null
        activeAction = null
        activeChannelResult = null
    }

    override fun getActiveScreen(): String? = activeScreen
}