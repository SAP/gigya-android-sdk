package com.gigya.android.sdk.nss

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import java.io.IOException

data class NssBuilder(var assetPath: String? = null,
                      var resultHandler: ResultHandler? = null) : NssObject() {

    private var flutterEngine: FlutterEngine? = null

    interface ResultHandler {

        fun onError(cause: String)
    }

    /**
     * Clear builder data.
     */
    fun clear() {
        assetPath = null
        resultHandler = null
    }

    /**
     * Load markup file from assets folder given filename/path.
     */
    private fun loadJsonFromAssets(context: Context, path: String) = try {
        context.assets.open(path).bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        null
    }

    /**
     * Use a cached engine.
     * Using a cached engine will improve screensets performance. Note that with a cached engine,
     * until manually killed, the engine will continue to run in the background.
     */
    fun cacheEngine(context: Context): NssBuilder {
        // Creating a new instance of the Flutter engine to be cached.
        flutterEngine = FlutterEngine(context)

        // Cache engine instance.
        FlutterEngineCache
                .getInstance()
                .put(GigyaNss.FLUTTER_ENGINE_ID, flutterEngine)
        return this
    }

    /**
     * Flush currently cached screensets engine.
     */
    fun flushCachedEngine() {
        flutterEngine?.destroy()
        flutterEngine = null
    }

    /**
     * Show screensets.
     */
    fun show(launcherContext: Context, initialRoute: String, handler: ResultHandler) {
        resultHandler = handler
        // Try to load from assets as a the default action.
        assetPath?.apply {

            val jsonAsset = loadJsonFromAssets(launcherContext, assetPath!!)
            jsonAsset.guard {
                throw RuntimeException("Failed to parse JSON File from assets folder")
            }

            // Start NssActivity using assets provided markup.
            NssActivity.start(
                    launcherContext,
                    flutterEngine != null,
                    markup = jsonAsset!!,
                    initialRoute = initialRoute)
        } ?: applyError("Asset path not available")
    }

    /**
     * Notify error using available result handler.
     */
    private fun applyError(cause: String) = resultHandler?.onError(cause)


}