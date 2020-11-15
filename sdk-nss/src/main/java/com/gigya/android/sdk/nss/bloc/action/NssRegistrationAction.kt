package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.nss.bloc.data.NssJsEvaluator
import com.gigya.android.sdk.nss.bloc.flow.INssFlowDelegate
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import com.gigya.android.sdk.nss.utils.serializeToMap
import io.flutter.plugin.common.MethodChannel

class NssRegistrationAction<T : GigyaAccount>(private val businessApi: IBusinessApiService<T>,
                                              jsEvaluator: NssJsEvaluator)
    : NssAction<T>(businessApi, jsEvaluator) {

    companion object {
        const val LOG_TAG = "NssRegistrationAction"
    }

    override fun initialize(expressions: Map<String, String>, result: MethodChannel.Result) {
        GigyaLogger.debug(NssLoginAction.LOG_TAG, "Explicit flow initialization ")

        jsEvaluator.eval(null, expressions) { jsResult ->
            result.success(mapOf("data" to mapOf(), "expressions" to jsEvaluator.mapExpressions(jsResult)))
        }
    }

    override fun onNext(method: String, arguments: MutableMap<String, Any>?) {
        flowDelegate.guard {
            GigyaLogger.error(NssLoginAction.LOG_TAG, "Action flow delegate not set")
        }

        // Call super to make sure "api" & "social" are covered.
        super.onNext(method, arguments)

        if (method == submit) {
            GigyaLogger.debug(LOG_TAG, "Starting registration flow with $method call")
            flowDelegate!!.refined<INssFlowDelegate<T>> {
                flattenArguments(arguments)
                businessApi.register(arguments, it.getMainFlowCallback())
            }
        }
    }


}