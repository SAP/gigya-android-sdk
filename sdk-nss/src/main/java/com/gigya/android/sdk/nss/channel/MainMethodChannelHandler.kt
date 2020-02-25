package com.gigya.android.sdk.nss.channel

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.nss.utils.refine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.*


class MainMethodChannelHandler(
        private val onRequestMarkup: () -> (String?),
        private val onRequestFlow: (String?) -> (Boolean),
        private val onRequestDismiss: () -> Unit) : MethodChannel.MethodCallHandler {

    companion object {

        const val LOG_TAG = "MainMethodChannelHandler"
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {

        GigyaLogger.debug(LOG_TAG, "Method = ${call.method}")

        when (call.method) {
            MainChannelCall.IGNITION.lowerCase() -> {
                onRequestMarkup()?.let { markup ->
                    result.success(markup)
                } ?: GigyaLogger.error(LOG_TAG, "Failed to initialize markup from asset file")
            }
            MainChannelCall.FLOW.lowerCase() -> {
                call.arguments.refine<Map<String, String>> {
                    val added = onRequestFlow(this["flowId"])
                    result.success(added)
                }
            }
            MainChannelCall.DISMISS.lowerCase() -> {
                onRequestDismiss()
                // No need to return a value to the channel in this case.
                // This method should dismiss the engine.
            }

        }
    }

    internal enum class MainChannelCall {
        IGNITION, FLOW, DISMISS;

        fun lowerCase() = this.name.toLowerCase(Locale.ENGLISH)
    }
}