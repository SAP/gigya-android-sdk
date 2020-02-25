package com.gigya.android.sdk.nss.channel

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel

internal interface IMethodChannel {

    var flutterMethodChannel: MethodChannel?

    fun initChannel(messenger: BinaryMessenger)
}

internal fun IMethodChannel.setMethodChannelHandler(handler: MethodChannel.MethodCallHandler) {
    flutterMethodChannel?.setMethodCallHandler(handler)
}