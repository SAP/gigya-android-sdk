package com.gigya.android.sdk.nss.bloc.events

import io.flutter.plugin.common.MethodChannel

data class FieldEventModel(val id: String, val oldVal: String?, val newVal: String?)

class ScreenEventsModel {

    var engineResponse: MethodChannel.Result? = null

    var nextRoute: String? = null

    var previousRoute: String? = null

    var data: MutableMap<String, Any> = mutableMapOf()

    fun `continue`() {
        engineResponse?.success(mutableMapOf(
                "sid" to nextRoute,
                "data" to data
        ))
    }

    fun showError(e: String) {
        engineResponse?.success(mutableMapOf("error" to e))
    }

}

