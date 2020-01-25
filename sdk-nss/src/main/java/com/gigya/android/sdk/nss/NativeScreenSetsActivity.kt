package com.gigya.android.sdk.nss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gigya.android.sdk.nss.channels.MainPlatformChannelHandler
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterMain

class NativeScreenSetsActivity : FlutterActivity() {

    private val mainPlatformChannelHandler: MainPlatformChannelHandler by lazy {
        MainPlatformChannelHandler()
    }

    companion object {

        const val LOG_TAG = "NativeScreenSetsActivity"

        // Extras.
        private const val EXTRA_INITIAL_ROUTE = "extra_initial_route"
        private const val EXTRA_MARKUP = "extra_markup"
        private const val EXTRA_CACHE_ENGINE = "extra_cache_engine"

        fun start(context: Context, withCachedEngine: Boolean = false, markup: String, initialRoute: String) {
            val intent: Intent = if (withCachedEngine) {
                NSSCachedEngineIntentBuilder().build(context)
            } else {
                NSSEngineIntentBuilder().build(context)
            }
            intent.putExtra(EXTRA_INITIAL_ROUTE, initialRoute)
            intent.putExtra(EXTRA_MARKUP, markup)
            intent.putExtra(EXTRA_CACHE_ENGINE, withCachedEngine)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initMainPlatformChannel(flutterEngine!!)

        // Execute the engine's "launch" method. Making sure that the main platform channel is already initialized
        // in the native side. Avoid signal -6 crash.
        flutterEngine!!.dartExecutor.executeDartEntrypoint(DartExecutor.DartEntrypoint(
                FlutterMain.findAppBundlePath(),
                "launch")
        )
    }

    /**
     * Open main communication method channel.
     */
    private fun initMainPlatformChannel(flutterEngine: FlutterEngine) {
        val mainChannel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, GigyaNss.CHANNEL_PLATFORM)
        mainChannel.setMethodCallHandler(mainPlatformChannelHandler)
    }

    //region Extensions

    /**
     * Wrapper inner class for attaching activity to a cached Flutter engine.
     */
    class NSSCachedEngineIntentBuilder :
            FlutterActivity.CachedEngineIntentBuilder(
                    NativeScreenSetsActivity::class.java,
                    GigyaNss.FLUTTER_ENGINE_ID
            )

    /**
     * Wrapper inner class for initializing a new Flutter engine.
     */
    class NSSEngineIntentBuilder :
            FlutterActivity.NewEngineIntentBuilder(
                    NativeScreenSetsActivity::class.java
            )

    //endregion
}