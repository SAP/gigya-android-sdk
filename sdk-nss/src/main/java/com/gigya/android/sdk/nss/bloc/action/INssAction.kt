package com.gigya.android.sdk.nss.bloc.action

import io.flutter.plugin.common.MethodChannel

interface INssAction {

    fun initialize(expressions: Map<String, String>, result: MethodChannel.Result)

    fun onNext(method: String, arguments: MutableMap<String, Any>?)
}