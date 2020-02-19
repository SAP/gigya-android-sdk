package com.gigya.android.sdk.nss.coordinator

import io.flutter.plugin.common.MethodChannel

interface NasCoordinatorLifecycle {

    fun onNext(method: String, arguments: Map<String, Any>?, result: MethodChannel.Result)

    fun onComplete()

    fun onDispose()
}