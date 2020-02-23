package com.gigya.android.sdk.nss.channel

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.nss.utils.refine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class ApiMethodChannelHandler(private val onApiRequested: (method: String, arguments: Map<String, Any>?, result: MethodChannel.Result) -> Unit)
    : MethodChannel.MethodCallHandler {

    companion object {

        const val LOG_TAG = "ApiMethodChannelHandler"
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        GigyaLogger.debug(LOG_TAG, "onMethodCall() with method: ${call.method}")

        call.arguments.refine<Map<String, Any>> {
            onApiRequested(call.method, this, result)
        }
    }
}