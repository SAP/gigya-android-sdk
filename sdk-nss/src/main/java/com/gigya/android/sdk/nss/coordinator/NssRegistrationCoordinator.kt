package com.gigya.android.sdk.nss.coordinator

import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.refine
import io.flutter.plugin.common.MethodChannel

class NssRegistrationCoordinator<T : GigyaAccount>(override val whenComplete: (withAction: String) -> Unit) : NssCoordinator<T>(whenComplete) {

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
                            val serializedObject = obj.serializeToMap()
                            result.success(serializedObject)

                            onComplete()
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
    }

    override fun onComplete() {
        whenComplete("")
    }

    override fun onDispose() {
        
    }


}