package com.gigya.android.sdk.nss.channel

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.nss.utils.refine
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel


class MainMethodChannelHandler(
        private val onInitFromAssets: () -> (String?),
        private val onFlowRequested: (String?) -> (Boolean),
        private val onFinish: () -> Unit) : MethodChannel.MethodCallHandler {

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
            MainChannelCall.FLOW.method -> {
                call.arguments.refine<Map<String, String>> {
                    val added = onFlowRequested(this["flowId"])
                    result.success(added)
                }
            }
            MainChannelCall.FINISH.method -> {
                onFinish
                // No need to return a value to the channel in this case.
                // This method should dismiss the engine.
            }

        }
    }

    internal enum class MainChannelCall(val method: String) {
        IGNITE("ignition"), FLOW("flow"), FINISH("finish")
    }
}