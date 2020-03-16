package com.gigya.android.sdk.nss

import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.channel.ApiMethodChannel
import com.gigya.android.sdk.nss.channel.ScreenMethodChannel
import com.gigya.android.sdk.nss.channel.dispose
import com.gigya.android.sdk.nss.channel.setMethodChannelHandler
import com.gigya.android.sdk.nss.coordinator.NssCoordinatorContainer
import com.gigya.android.sdk.nss.flows.NssFlow
import com.gigya.android.sdk.nss.flows.NssFlowFactory
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refine
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class NssViewModel<T : GigyaAccount>(
        private val mScreenChannel: ScreenMethodChannel,
        private val mApiChannel: ApiMethodChannel,
        private val mFlowFactory: NssFlowFactory<T>)
    : NssCoordinatorContainer<T>() {

    var mFinish: () -> Unit? = { }
    var mEvent: NssEvents? = null

    companion object {

        const val LOG_TAG = "NssViewModel"
    }

    internal fun dispose() {
        mEvent = null
        mScreenChannel.dispose()
        mApiChannel.dispose()
    }

    fun loadChannels(engine: FlutterEngine) {
        mScreenChannel.initChannel(engine.dartExecutor.binaryMessenger)
        mScreenChannel.setMethodChannelHandler(mScreenMethodChannelHandler)

        mApiChannel.initChannel(engine.dartExecutor.binaryMessenger)
        mApiChannel.setMethodChannelHandler(mApiMethodChannelHandler)
    }

    private val mScreenMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, result ->
            when (call.method) {
                ScreenMethodChannel.ScreenCall.FLOW.lowerCase() -> {
                    call.arguments.refine<Map<String, String>> {
                        val flowId = this["flowId"]
                        val flow = mFlowFactory.createFor(flowId!!).guard {
                            mEvent?.onException("Failed to initialize flow")
                        }
                        flow.refine<NssFlow<T>> {
                            add(flowId, this)
                            result.success(true)
                        }
                    }
                }
                ScreenMethodChannel.ScreenCall.DISMISS.lowerCase() -> {
                    clear()
                    mFinish()
                }
            }
        }
    }

    private val mApiMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, result ->
            call.arguments.refine<Map<String, Any>> {
                getCurrent()?.onNext(call.method, this, result)
            }
        }
    }

}