package com.gigya.android.sdk.nss

import android.content.Context
import com.gigya.android.sdk.nss.channels.MainPlatformChannelHandler
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class NativeScreenSetsActivity : FlutterActivity() {

    private lateinit var mainPlatformChannelHandler: MainPlatformChannelHandler

    companion object {

        const val LOG_TAG = "NativeScreenSetsActivity"

        const val FLUTTER_ENGINE_ID = "nss_engine_id"

        fun launch(context: Context) {
            val intent = NSSEngineIntentBuilder().build(context)
            context.startActivity(intent)
        }
    }

    //region Extensions

    /**
     * Wrapper inner class for attaching activity to a cached Flutter engine.
     */
    internal class NSSCachedEngineIntentBuilder :
            FlutterActivity.CachedEngineIntentBuilder(
                    NativeScreenSetsActivity::class.java,
                    FLUTTER_ENGINE_ID
            )

    /**
     * Wrapper inner class for initializing a new Flutter engine.
     */
    internal class NSSEngineIntentBuilder :
            FlutterActivity.NewEngineIntentBuilder(
                    NativeScreenSetsActivity::class.java
            )

    //endregion

    //region Flutter engine

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        initMainPlatformChannel(flutterEngine)
    }

    /**
     * Open main communication method channel.
     */
    private fun initMainPlatformChannel(flutterEngine: FlutterEngine) {
        val mainChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, GigyaNss.CHANNEL_PLATFORM)
        mainPlatformChannelHandler = MainPlatformChannelHandler()
        mainChannel.setMethodCallHandler(mainPlatformChannelHandler)
    }

    //endregion

}