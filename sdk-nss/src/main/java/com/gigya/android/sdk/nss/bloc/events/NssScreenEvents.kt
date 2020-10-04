package com.gigya.android.sdk.nss.bloc.events

abstract class NssScreenEvents {

    open fun screenDidLoad() {}

    open fun routeFrom(screen: ScreenRouteFromModel) {
        screen.next()
    }

    open fun routeTo(screen: ScreenRouteToModel) {
        screen.next()
    }

    open fun submit(screen: ScreenSubmitModel) {
        screen.next()
    }

    open fun fieldDidChange(screen: ScreenFieldModel, field: FieldEventModel) {
        screen.next()
    }

}



