package com.gigya.android.sdk.nss.engine

import android.content.Context
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.nss.NssActivity
import com.gigya.android.sdk.nss.channel.IgnitionMethodChannel
import io.flutter.embedding.android.FlutterFragment
import io.flutter.embedding.android.FlutterView
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.view.FlutterMain

/**
 * Flutter engine lifecycle helper.
 */
open class NssEngineLifeCycle {

    companion object {
        const val FLUTTER_ENGINE_ID = "nss_engine_id"
        const val DART_ENTRY_POINT = "main"
    }

    private fun newEngine() = FlutterEngine(Gigya.getContainer().get(Context::class.java))

    private fun existsInTempCache() = FlutterEngineCache.getInstance().contains(FLUTTER_ENGINE_ID)

    private fun addToTempCache(engine: FlutterEngine) = FlutterEngineCache.getInstance().put(FLUTTER_ENGINE_ID, engine)

    private fun registerIgnitionChannel(engine: FlutterEngine) {
        val ignitionChannel = Gigya.getContainer().get(IgnitionMethodChannel::class.java)
        ignitionChannel.initChannel(engine.dartExecutor.binaryMessenger)
    }

    fun getNssEngine(): FlutterEngine? = FlutterEngineCache.getInstance().get(FLUTTER_ENGINE_ID)

    /**
     * Initialize a new Flutter engine.
     */
    open fun initializeEngine() {
        if (!existsInTempCache()) {
            val engine = newEngine()
            registerIgnitionChannel(engine)
            addToTempCache(engine)
        }
    }

    /**
     * Execute main Flutter engine Dart entry point.
     */
    fun engineExecuteMain() {
        FlutterEngineCache.getInstance().get(FLUTTER_ENGINE_ID)?.dartExecutor?.executeDartEntrypoint(
                DartExecutor.DartEntrypoint(
                        FlutterMain.findAppBundlePath(),
                        DART_ENTRY_POINT
                )
        )
    }

    /**
     * Destroy and remove current Flutter engine from cache.
     */
    fun disposeEngine() {
        val engine = FlutterEngineCache.getInstance().get(FLUTTER_ENGINE_ID)
        engine?.destroy()
        FlutterEngineCache
                .getInstance().remove(FLUTTER_ENGINE_ID)
    }

    /**
     * Get current Flutter engine fragment using the cached engine.
     */
    fun getEngineFragment(): FlutterFragment {
        return FlutterFragment.withCachedEngine(FLUTTER_ENGINE_ID)
                .transparencyMode(FlutterView.TransparencyMode.transparent)
                .shouldAttachEngineToActivity(true)
                .build()
    }

    open fun show(context: Context, markup: Map<String, Any>) {
        NssActivity.start(context, markup)
    }
}