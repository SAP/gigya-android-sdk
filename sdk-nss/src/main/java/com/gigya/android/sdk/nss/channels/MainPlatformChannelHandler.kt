package com.gigya.android.sdk.nss.channels

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.nss.NativeScreenSetsActivity
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class MainPlatformChannelHandler: MethodChannel.MethodCallHandler {

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {

        GigyaLogger.debug(NativeScreenSetsActivity.LOG_TAG, "Method = ${call.method}")

        when (call.method) {
            "engineInit" -> result.success(mapOf("responseId" to "engineInit"))
        }
    }
}