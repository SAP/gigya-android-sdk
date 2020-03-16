package com.gigya.android.sdk.nss.channel

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import java.util.*

class ScreenMethodChannel : IMethodChannel {

    override var flutterMethodChannel: MethodChannel? = null

    override fun initChannel(messenger: BinaryMessenger) {
        flutterMethodChannel = MethodChannel(messenger, "gigya_nss_engine/method/screen")
    }

    internal enum class ScreenCall {
        FLOW, DISMISS;

        fun lowerCase() = this.name.toLowerCase(Locale.ENGLISH)
    }
}