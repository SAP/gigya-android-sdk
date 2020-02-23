package com.gigya.android.sdk.nss

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.channel.ApiMethodChannelHandler
import com.gigya.android.sdk.nss.channel.MainMethodChannelHandler
import com.gigya.android.sdk.nss.coordinator.NssCoordinatorContainer
import com.gigya.android.sdk.nss.flows.NssFlowFactory
import com.gigya.android.sdk.nss.utils.guard
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class NssViewModel<T : GigyaAccount>(private val markup: String, private val finish: () -> Unit) : NssCoordinatorContainer<T>() {

    companion object {

        const val LOG_TAG = "NssViewModel"
    }

    fun registerMethodChannels(engine: FlutterEngine) {
        // Register main channel.
        MethodChannel(engine.dartExecutor.binaryMessenger, GigyaNss.CHANNEL_MAIN)
                .setMethodCallHandler(mMainMethodChannelHandler)

        // Register AP channel.
        MethodChannel(engine.dartExecutor.binaryMessenger, GigyaNss.CHANNEL_API)
                .setMethodCallHandler(mApiMethodChannelHandler)
    }

    private val mMainMethodChannelHandler: MainMethodChannelHandler by lazy {
        MainMethodChannelHandler(
                onInitFromAssets = {
                    GigyaLogger.debug(LOG_TAG, "Markup available - convert to map for channel init")
                    markup
                },
                onFlowRequested = { flowId ->
                    flowId.guard {
                        throw RuntimeException("Failed to inject flowId. Flow coordination is mandatory.")
                    }

                    val flow = NssFlowFactory.createFor<T>(flowId!!).guard {
                        throw RuntimeException("Failed to initialize flow")
                    }

                    add(flowId, flow!!)
                    true
                },
                onDismiss = {
                    GigyaLogger.debug(LOG_TAG, "onFinish received from engine.")
                    finish()
                }
        )
    }

    private val mApiMethodChannelHandler: ApiMethodChannelHandler by lazy {
        ApiMethodChannelHandler(
                onApiRequested = { method, arguments, result ->
                    getCurrent()?.onNext(method, arguments, result)
                })
    }


}