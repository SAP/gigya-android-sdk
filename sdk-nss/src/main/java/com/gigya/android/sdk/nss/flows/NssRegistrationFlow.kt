package com.gigya.android.sdk.nss.flows

import android.util.Log
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.utils.serializeToMap
import io.flutter.plugin.common.MethodChannel

class NssRegistrationFlow<T : GigyaAccount>(override val bApi: IBusinessApiService<T>) : NssFlow<T>(bApi) {

    companion object {
        const val LOG_TAG = "NssRegistrationFlow"
        const val SUBMIT_API = "accounts.register"
    }

    override fun onNext(method: String, arguments: Map<String, Any>?, result: MethodChannel.Result) {
        super.onNext(method, arguments, result)
        when (method) {
            "accounts.register" -> {
                GigyaLogger.debug(LOG_TAG, "Starting registration flow with $method call")

                bApi.register(arguments, object : GigyaLoginCallback<T>() {
                    override fun onSuccess(obj: T) {
                        val serializedObject = obj.serializeToMap(gson)
                        result.success(serializedObject)
                    }

                    override fun onError(error: GigyaError?) {
                        error?.let {
                            result.error(it.errorCode.toString(), it.localizedMessage, it.data)
                        }
                    }

                })
            }
            else -> {
                // Flow cannot handle method.8
                Log.e("GigyaError", "Current flow cannot handle this request. Verify your flow ids are correct")
            }
        }
    }

    override fun onDispose() {
        // Stub.
    }


}