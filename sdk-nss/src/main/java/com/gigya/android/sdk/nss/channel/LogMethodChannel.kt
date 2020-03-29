package com.gigya.android.sdk.nss.channel

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import java.util.*

class LogMethodChannel : IMethodChannel {

    override var flutterMethodChannel: MethodChannel? = null

    override fun initChannel(messenger: BinaryMessenger) {
        flutterMethodChannel = MethodChannel(messenger, "gigya_nss_engine/method/log")
    }

    internal enum class LogCall(val identifier: String) {
        DEBUG("debug"), ERROR("error");
    }
}