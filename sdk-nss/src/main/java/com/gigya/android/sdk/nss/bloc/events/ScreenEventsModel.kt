package com.gigya.android.sdk.nss.bloc.events

import io.flutter.plugin.common.MethodChannel

data class FieldEventModel(val id: String, val oldVal: String?, val newVal: String?)

open class ScreenEventsModel() : ScreenRouteFromModel, ScreenRouteToModel, ScreenSubmitModel, ScreenFieldModel {

    var engineResponse: MethodChannel.Result? = null

    var nr: String? = null

    var pr: String? = null

    var data: MutableMap<String, Any> = mutableMapOf()

    /**
     * Release the event.
     * Next must be called in order to release the waiting state of the engine.
     * A 10 second timeout applies.
     */
    private fun releaseEngine() {
        engineResponse?.success(mutableMapOf(
                "sid" to nr,
                "data" to data
        ))
    }

    private fun error(e: String) {
        engineResponse?.success(mutableMapOf("error" to e))
    }

    override fun next() {
        releaseEngine()
    }

    override fun previousRoute() = pr

    override fun nextRoute(): String? = nr

    override fun nextRoute(route: String) {
        nr = route
    }

    override fun screenData() = data

    override fun showError(s: String) {
        error(s)
    }

}

interface ScreenRouteFromModel {

    fun next()

    fun previousRoute(): String?

    fun screenData(): Map<String, Any>?
}

interface ScreenRouteToModel {

    fun next()

    fun nextRoute(): String?

    fun nextRoute(route: String)

    fun screenData(): MutableMap<String, Any>?
}

interface ScreenSubmitModel {

    fun next()

    fun screenData(): MutableMap<String, Any>?

    fun showError(s: String)
}

interface ScreenFieldModel {

    fun next()

    fun showError(s: String)
}

