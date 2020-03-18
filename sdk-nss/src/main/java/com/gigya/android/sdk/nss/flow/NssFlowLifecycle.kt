package com.gigya.android.sdk.nss.flow

import io.flutter.plugin.common.MethodChannel

interface NssFlowLifecycle {

    fun initialize(result: MethodChannel.Result)

    fun onNext(method: String, arguments: Map<String, Any>?, result: MethodChannel.Result) {
        if (method == "api") {
            // Send anonymous api.
        }
    }

    fun onDispose()
}