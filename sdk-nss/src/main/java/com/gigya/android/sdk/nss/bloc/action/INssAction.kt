package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.nss.bloc.action.NssAction.Companion.api
import io.flutter.plugin.common.MethodChannel

interface INssAction {

    fun initialize(result: MethodChannel.Result)

    fun onNext(method: String, arguments: MutableMap<String, Any>?) {
        if (method == api) {
            // Send anonymous api.
        }
    }
}