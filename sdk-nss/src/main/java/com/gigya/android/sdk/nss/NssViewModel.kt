package com.gigya.android.sdk.nss

import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.channel.ApiMethodChannel
import com.gigya.android.sdk.nss.channel.MainMethodChannel
import com.gigya.android.sdk.nss.channel.setMethodChannelHandler
import com.gigya.android.sdk.nss.coordinator.NssCoordinatorContainer
import com.gigya.android.sdk.nss.flows.NssFlow
import com.gigya.android.sdk.nss.flows.NssFlowFactory
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refine
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.util.*

class NssViewModel<T : GigyaAccount>(
        private val mainChannel: MainMethodChannel,
        private val apiChannel: ApiMethodChannel,
        private val flowFactory: NssFlowFactory<T>)
    : NssCoordinatorContainer<T>() {

    var markup: String? = null
    var finish: () -> Unit? = { }
    var initialRoute: String? = null
    var events: NssEvents? = null

    companion object {

        const val LOG_TAG = "NssViewModel"
    }

    internal fun dispose() {
        markup = null
        initialRoute = null
    }

    fun loadChannels(engine: FlutterEngine) {

        mainChannel.initChannel(engine.dartExecutor.binaryMessenger)
        mainChannel.setMethodChannelHandler(mMainMethodChannelHandler)

        apiChannel.initChannel(engine.dartExecutor.binaryMessenger)
        apiChannel.setMethodChannelHandler(mApiMethodChannelHandler)
    }

    internal enum class MainCall {
        IGNITION, FLOW, DISMISS;

        fun lowerCase() = this.name.toLowerCase(Locale.ENGLISH)
    }

    private val mMainMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, result ->
            when (call.method) {
                MainCall.IGNITION.lowerCase() -> {
                    markup?.let {
                        result.success(it)
                    } ?: events?.onException("Markup unavailable?!")
                }
                MainCall.FLOW.lowerCase() -> {
                    call.arguments.refine<Map<String, String>> {
                        val flowId = this["flowId"]
                        val flow = flowFactory.createFor(flowId!!).guard {
                            events?.onException("Failed to initialize flow")
                        }
                        flow.refine<NssFlow<T>> {
                            add(flowId, this)
                            result.success(true)
                        }
                    }
                }
                MainCall.DISMISS.lowerCase() -> {
                    clear()
                    finish()
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