package com.gigya.android.sdk.nss.flows

import io.flutter.plugin.common.MethodChannel

interface NssFlowLifecycle {

    fun onNext(method: String, arguments: Map<String, Any>?, result: MethodChannel.Result)

    fun onDispose()
}