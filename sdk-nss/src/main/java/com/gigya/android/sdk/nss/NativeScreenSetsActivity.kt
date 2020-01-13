package com.gigya.android.sdk.nss

import android.content.Context
import android.os.Bundle
import com.gigya.android.sdk.GigyaLogger
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel

class NativeScreenSetsActivity : FlutterActivity() {

    companion object {

        const val LOG_TAG = "NativeScreenSetsActivity"

        const val FLUTTER_ENGINE_ID = "nss_engine_id"

        fun launch(context: Context, materialOnly: Boolean) {
            val intent = NSSEngineIntentBuilder().build(context)
            intent.putExtra("materialOnly", materialOnly)
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

    //region Lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        var materialOnly = true
        intent.extras?.let { bundle ->
            materialOnly = bundle.getBoolean("materialOnly", true)
        }

        // Register main method channel & method handler.
        MethodChannel(
                flutterEngine!!.dartExecutor.binaryMessenger,
                GigyaNss.CHANNEL_PLATFORM)
                .setMethodCallHandler { call, result ->

                    GigyaLogger.debug(LOG_TAG, "Method = ${call.method}")

                    when (call.method) {
                        "engineInit" -> result.success(
                                mapOf("responseId" to "engineInit", "platformAware" to materialOnly)
                        )
                    }
                }
    }

    //endregion

}