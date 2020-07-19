package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.nss.bloc.flow.INssFlowDelegate
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import io.flutter.plugin.common.MethodChannel

class NssRegistrationAction<T : GigyaAccount>(private val businessApi: IBusinessApiService<T>) : NssAction<T>(businessApi) {

    companion object {
        const val LOG_TAG = "NssRegistrationAction"
    }

    override fun initialize(result: MethodChannel.Result) {
        GigyaLogger.debug(NssLoginAction.LOG_TAG, "Explicit flow initialization ")
        result.success(mapOf<String, Any>())
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