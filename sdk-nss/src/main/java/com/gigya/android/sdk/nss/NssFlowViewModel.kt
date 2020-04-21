package com.gigya.android.sdk.nss

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.bloc.action.NssAction
import com.gigya.android.sdk.nss.bloc.action.NssActionFactory
import com.gigya.android.sdk.nss.channel.*
import com.gigya.android.sdk.nss.bloc.flow.NssFlowManager
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class NssFlowViewModel<T : GigyaAccount>(
        private val mScreenChannel: ScreenMethodChannel,
        private val mApiChannel: ApiMethodChannel,
        private val mLogChannel: LogMethodChannel,
        private val mFlowManager: NssFlowManager<T>) {

    var mFinish: () -> Unit? = { }
    var mEvent: NssEvents<T>? = null

    companion object {

        const val LOG_TAG = "NssViewModel"
    }

    internal fun dispose() {
        mEvent = null
        mScreenChannel.dispose()
        mApiChannel.dispose()
    }

    /**
     * Register Flutter engine specific communication channels used for Nss usage.
     */
    fun loadChannels(engine: FlutterEngine) {
        mScreenChannel.initChannel(engine.dartExecutor.binaryMessenger)
        mScreenChannel.setMethodChannelHandler(mScreenMethodChannelHandler)

        mApiChannel.initChannel(engine.dartExecutor.binaryMessenger)
        mApiChannel.setMethodChannelHandler(mApiMethodChannelHandler)

        mLogChannel.initChannel(engine.dartExecutor.binaryMessenger)
        mLogChannel.setMethodChannelHandler(mLogMethodChannelHandler)
    }

    private val mLogMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, _ ->
            call.arguments.refined<Map<String, String>> { logMap ->
                when (call.method) {
                    LogMethodChannel.LogCall.DEBUG.identifier -> {
                        GigyaLogger.debug(logMap["tag"], logMap["message"])
                    }
                    LogMethodChannel.LogCall.ERROR.identifier -> {
                        GigyaLogger.error(logMap["tag"], logMap["message"])
                    }
                }
            }
        }
    }

    private val mScreenMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, result ->
            when (call.method) {
                ScreenMethodChannel.ScreenCall.ACTION.identifier -> {
                    call.arguments.refined<Map<String, String>> { map ->
                        val actionId = map["actionId"]
                        actionId.guard {
                            GigyaLogger.error(LOG_TAG, "Missing action if in screen action initializer")
                            throw RuntimeException("Missing action if in screen action initializer. Unable to generate the correct action")
                        }
                        mFlowManager.setCurrent(actionId!!, result)
                    }
                }
                ScreenMethodChannel.ScreenCall.DISMISS.identifier -> {
                    mFlowManager.dispose()
                    mFinish()
                }
            }
        }
    }

    private val mApiMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, result ->
            call.arguments.refined<MutableMap<String, Any>> { args ->
                mFlowManager.onNext(call.method, args, result)
            }
        }
    }

}