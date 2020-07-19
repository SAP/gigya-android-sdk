package com.gigya.android.sdk.nss.channel

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import java.util.*

class ScreenMethodChannel : IMethodChannel {

    override var flutterMethodChannel: MethodChannel? = null

    override fun initChannel(messenger: BinaryMessenger) {
        Enum
        flutterMethodChannel = MethodChannel(messenger, "gigya_nss_engine/method/screen")
    }

    internal enum class ScreenCall(val identifier: String) {
        ACTION("action"),
        DISMISS("_dismiss"),
        CANCEL("_cancel"),
        LINK("link")
    }
}