package com.gigya.android.sdk.nss

import android.content.Context
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity

class NativeScreenSetsActivity : FlutterActivity() {

    companion object {

        const val FLUTTER_ENGINE_ID = "nss_engine_id"

        fun launch(context: Context) {
            context.startActivity(
                    NSSEngineIntentBuilder().build(context)
            )
        }

        const val METHOD_CHANNEL_ID_MAIN = "gigya_nss_engine/method/main"
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register main method channel.
    }

}