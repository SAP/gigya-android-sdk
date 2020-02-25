package com.gigya.android.sdk.nss.flows

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.utils.refine
import com.gigya.android.sdk.nss.utils.serializeToMap
import io.flutter.plugin.common.MethodChannel

class NssLoginFlow<T : GigyaAccount>(override val bApi: IBusinessApiService<T>) : NssFlow<T>(bApi) {

    companion object {
        const val LOG_TAG = "NssLoginFlow"
    }

    override fun onNext(method: String, arguments: Map<String, Any>?, result: MethodChannel.Result) {
        when (method) {
            "accounts.login" -> {
                GigyaLogger.debug(LOG_TAG, "Starting login flow with $method call")

                bApi.login(arguments, object : GigyaLoginCallback<T>() {

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
        }
    }

    override fun onDispose() {
        // Stub.
    }
}