package com.gigya.android.sdk.nss.bloc.events

abstract class NssScreenEvents() {

    open fun screenDidLoad() {}

    open fun routeFrom(screen: ScreenEventsModel) {}

    open fun routeTo(screen: ScreenEventsModel) {}

    open fun submit(screen: ScreenEventsModel) {}

    open fun fieldDidChange(screen: ScreenEventsModel, field: FieldEventModel) { }

}
