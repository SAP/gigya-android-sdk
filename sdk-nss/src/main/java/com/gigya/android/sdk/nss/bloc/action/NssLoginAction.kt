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

class NssLoginAction<T : GigyaAccount>(private val businessApi: IBusinessApiService<T>,
                                       jsEvaluator: NssJsEvaluator)
    : NssAction<T>(businessApi, jsEvaluator) {

    companion object {
        const val LOG_TAG = "NssLoginAction"
    }

    override fun initialize(expressions: Map<String, String>, result: MethodChannel.Result) {
        GigyaLogger.debug(LOG_TAG, "Explicit flow initialization ")
        super.initialize(expressions, result)
    }

    override fun onNext(method: String, arguments: MutableMap<String, Any>?) {
        flowDelegate.guard {
            GigyaLogger.error(LOG_TAG, "Action flow delegate not set")
        }

        // Call super to make sure "api" & "social" are covered.
        super.onNext(method, arguments)

        if (method == submit) {
            GigyaLogger.debug(LOG_TAG, "Starting login flow with $method call")
            flowDelegate!!.refined<INssFlowDelegate<T>> {
                businessApi.login(arguments, it.getMainFlowCallback())
            }

        }
    }
}