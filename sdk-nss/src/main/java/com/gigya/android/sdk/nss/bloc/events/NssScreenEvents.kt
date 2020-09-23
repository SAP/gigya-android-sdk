package com.gigya.android.sdk.nss.bloc.events

abstract class NssScreenEvents {

    open fun screenDidLoad() {}

    open fun routeFrom(screen: ScreenEventsModel) {
        screen.`continue`();
    }

    open fun routeTo(screen: ScreenEventsModel) {
        screen.`continue`();
    }

    open fun submit(screen: ScreenEventsModel) {
        screen.`continue`();
    }

    open fun fieldDidChange(screen: ScreenEventsModel, field: FieldEventModel) {
        screen.`continue`();
    }

}
