package com.gigya.android.sdk.nss.bloc.events

import io.flutter.plugin.common.MethodChannel

class ScreenEventsManager {

    private val screenMapper: MutableMap<String, NssScreenEvents> = mutableMapOf()

    fun addFor(screenId: String, events: NssScreenEvents) {
        screenMapper[screenId] = events
    }

    fun eventsFor(screenId: String): NssScreenEvents? {
        return screenMapper[screenId]
    }

    fun disposeResult(result: MethodChannel.Result) {
        result.success(mapOf<String, Any>())
    }

    fun dispose() {
        screenMapper.clear()
    }
}