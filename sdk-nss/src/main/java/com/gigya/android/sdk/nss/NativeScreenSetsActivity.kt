package com.gigya.android.sdk.nss

import android.content.Context
import io.flutter.embedding.android.FlutterActivity

class NativeScreenSetsActivity : FlutterActivity() {

    companion object {

        const val FLUTTER_ENGINE_ID = "nss_engine_id"

        fun launch(context: Context) {
            context.startActivity(
                    NSSEngineIntentBuilder().build(context)
            )
        }
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
}