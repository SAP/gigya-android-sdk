package com.gigya.android.sdk.nss.flows

import android.util.Log
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.utils.refine
import com.gigya.android.sdk.nss.utils.serializeToMap
import io.flutter.plugin.common.MethodChannel

class NssRegistrationFlow<T : GigyaAccount>(override val id: String) : NssFlow<T>(id) {

    companion object {

        const val LOG_TAG = "NssRegistrationCoordinator"
    }

    // The code business api will always be available to the library.
    private var mBusinessApiService: IBusinessApiService<*>? = Gigya.getContainer().get(IBusinessApiService::class.java)

    override fun onNext(method: String, arguments: Map<String, Any>?, result: MethodChannel.Result) {
        when (method) {
            "accounts.register" -> {
                GigyaLogger.debug(LOG_TAG, "Starting registration flow with $method call")

                mBusinessApiService.refine<IBusinessApiService<T>> {
                    this.register(arguments, object : GigyaLoginCallback<T>() {

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
            else -> {
                // Flow cannot handle method.8
                Log.e("GigyaError", "Current flow cannot handle this request. Verify your flow ids are correct")
            }
        }
    }

    override fun onDispose() {

    }


}