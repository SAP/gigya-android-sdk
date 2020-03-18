package com.gigya.android.sdk.nss.flows

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import com.gigya.android.sdk.nss.utils.serializeToMap
import io.flutter.plugin.common.MethodChannel

class NssLoginFlow<T : GigyaAccount>(override val bApi: IBusinessApiService<T>) : NssFlow<T>(bApi) {

    companion object {
        const val LOG_TAG = "NssLoginFlow"
    }

    override fun initialize(result: MethodChannel.Result) {
        GigyaLogger.debug(LOG_TAG, "Explicit flow initialization ")
        result.success(mapOf<String, Any>())
    }

    override fun onNext(method: String, arguments: Map<String, Any>?, result: MethodChannel.Result) {
        super.onNext(method, arguments, result)
        if (method == "submit") {
            arguments?.get("params")?.guard {
                throw RuntimeException("Submission of flow requires argument parameters!")
            }

            arguments!!["params"].refined<MutableMap<String, Any>> { params ->
                GigyaLogger.debug(LOG_TAG, "Starting login flow with $method call")
                bApi.login(params, object : GigyaLoginCallback<T>() {

                    override fun onSuccess(obj: T) {
                        val serializedObject = obj.serializeToMap(gson)
                        result.success(serializedObject)
                    }

                    override fun onError(error: GigyaError?) {
                        error?.let {
                            result.error(
                                    it.errorCode.toString(),
                                    it.localizedMessage,
                                    it.data
                            )
                        }
                    }

                })
            }
        }

    }

    override fun onDispose() {
        // Stub.
    }
}