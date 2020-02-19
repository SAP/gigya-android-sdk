package com.gigya.android.sdk.nss

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.channel.ApiMethodChannelHandler
import com.gigya.android.sdk.nss.channel.MainMethodChannelHandler
import com.gigya.android.sdk.nss.coordinator.NssCoordinator
import com.gigya.android.sdk.nss.coordinator.NssCoordinatorFactory
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class NssViewModel<T : GigyaAccount>(private val markup: String) {

    companion object {

        const val LOG_TAG = "NssViewModel"
    }

    /**
     * Main flow [NssCoordinator] which encapsulates the entire flow.
     * The main coordinator can instantiate additional inline coordinators.
     */
    var mCoordinator: NssCoordinator<T>? = null

    fun registerMethodChannels(engine: FlutterEngine) {
        // Register main channel.
        MethodChannel(engine.dartExecutor.binaryMessenger, GigyaNss.CHANNEL_MAIN)
                .setMethodCallHandler(mMainMethodChannelHandler)

        // Register AP channel.
        MethodChannel(engine.dartExecutor.binaryMessenger, GigyaNss.CHANNEL_API)
                .setMethodCallHandler(mApiMethodChannelHandler)
    }

    private val mMainMethodChannelHandler: MainMethodChannelHandler by lazy {
        MainMethodChannelHandler(onInitFromAssets = {
            GigyaLogger.debug(LOG_TAG, "Markup available - convert to map for channel init")
            markup
        })
    }

    private val mApiMethodChannelHandler: ApiMethodChannelHandler by lazy {
        ApiMethodChannelHandler(onApiRequested = { method, arguments, result ->
            if (mCoordinator == null) {
                mCoordinator = NssCoordinatorFactory.createFor(method, whenComplete = { action ->
                    coordinatorCompleted(withAction = action)
                })
            }
            mCoordinator?.onNext(method, arguments, result)
        })
    }

    private fun coordinatorCompleted(withAction: String) {
        // Dispose coordinator.
        mCoordinator = null
    }

}