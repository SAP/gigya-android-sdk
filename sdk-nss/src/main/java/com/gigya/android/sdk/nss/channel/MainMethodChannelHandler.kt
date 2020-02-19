package com.gigya.android.sdk.nss.channel

import com.gigya.android.sdk.GigyaLogger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel


class MainMethodChannelHandler(private val onInitFromAssets: () -> (String?)) : MethodChannel.MethodCallHandler {

    companion object {

        const val LOG_TAG = "MainMethodChannelHandler"
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {

        GigyaLogger.debug(LOG_TAG, "Method = ${call.method}")

        when (call.method) {
            MainChannelCall.IGNITE.method -> {
                onInitFromAssets()?.let { markup ->
                    result.success(markup)
                } ?: GigyaLogger.error(LOG_TAG, "Failed to initialize markup from asset file")
            }
        }
    }

    internal enum class MainChannelCall(val method: String) {
        IGNITE("ignition")
    }
}