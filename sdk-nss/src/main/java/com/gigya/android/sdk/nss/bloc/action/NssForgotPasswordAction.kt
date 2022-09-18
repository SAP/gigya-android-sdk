package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.bloc.data.NssJsEvaluator
import com.gigya.android.sdk.nss.bloc.flow.INssFlowDelegate
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import com.gigya.android.sdk.nss.utils.serializeToMap
import io.flutter.plugin.common.MethodChannel

class NssForgotPasswordAction<T : GigyaAccount>(private val businessApi: IBusinessApiService<T>,
                                                jsEvaluator: NssJsEvaluator)
    : NssAction<T>(businessApi, jsEvaluator) {

    companion object {
        const val LOG_TAG = "NssForgotPasswordAction"
    }

    override fun initialize(expressions: Map<String, String>, result: MethodChannel.Result) {
        GigyaLogger.debug(NssLoginAction.LOG_TAG, "Explicit flow initialization ")
        super.initialize(expressions, result)
    }

    override fun onNext(method: String, arguments: MutableMap<String, Any>?) {
        flowDelegate.guard {
            GigyaLogger.error(NssLoginAction.LOG_TAG, "Action flow delegate not set")
        }

        // Call super to make sure "api".
        super.onNext(method, arguments)

        if (method == submit) {
            GigyaLogger.debug(NssRegistrationAction.LOG_TAG, "Password reset email requested")
            flowDelegate!!.refined<INssFlowDelegate<T>> {
                flattenArguments(arguments)
                businessApi.forgotPassword(arguments, object : GigyaCallback<GigyaApiResponse>() {
                    override fun onSuccess(obj: GigyaApiResponse?) {
                        it.getMainFlowCallback()?.onGenericResponse(obj)
                    }

                    override fun onError(error: GigyaError?) {
                        it.getMainFlowCallback()?.onError(error)
                    }
                })
            }
        }
    }

}