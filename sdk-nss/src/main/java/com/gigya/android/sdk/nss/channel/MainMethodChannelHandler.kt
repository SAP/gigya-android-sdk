package com.gigya.android.sdk.nss.channel

import com.gigya.android.sdk.GigyaLogger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel


class MainMethodChannelHandler(private val onInitFromAssets: () -> (HashMap<String, Any>?)) : MethodChannel.MethodCallHandler {

    companion object {

        const val LOG_TAG = "MainMethodChannelHandler"
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {

        GigyaLogger.debug(LOG_TAG, "Method = ${call.method}")

        when (call.method) {
            MainChannelCall.INIT.method -> {
                onInitFromAssets()?.let { mappedMarkup ->
                    result.success(mappedMarkup)
                } ?: GigyaLogger.error(LOG_TAG, "Failed to initialize markup from asset file")
            }
        }
    }

    internal enum class MainChannelCall(val method: String) {
        INIT("initialize")
    }
}