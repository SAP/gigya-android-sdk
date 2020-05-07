package com.gigya.android.sdk.nss.channel

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel

class IgnitionMethodChannel : IMethodChannel {

    override var flutterMethodChannel: MethodChannel? = null

    override fun initChannel(messenger: BinaryMessenger) {
        flutterMethodChannel = MethodChannel(messenger, "gigya_nss_engine/method/ignition")
    }

    internal enum class IgnitionCall(val identifier: String) {
        IGNITION("ignition"), READY_FOR_DISPLAY("ready_for_display")
    }
}