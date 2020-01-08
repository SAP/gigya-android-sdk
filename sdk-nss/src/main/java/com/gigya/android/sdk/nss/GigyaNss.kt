package com.gigya.android.sdk.nss

import android.app.Activity

object GigyaNss {

    fun showScreenSet(launcher: Activity) {
        NativeScreenSetsActivity.launch(launcher)
    }
}