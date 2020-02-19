package com.gigya.android.sdk.nss

import android.content.Context
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import java.io.IOException

data class NssBuilder<T : GigyaAccount>(var assetPath: String? = null, var resultHandler: ResultHandler? = null) {

    companion object {

        const val LOG_TAG = "NssBuilder"
    }

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
    private fun loadJsonFromAssets(context: Context, fileName: String) = try {
        GigyaLogger.debug(LOG_TAG, "loadJsonFromAssets() with fileName $fileName")
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        null
    }

    /**
     * Use a cached engine.
     * Using a cached engine will improve screensets performance. Note that with a cached engine,
     * until manually killed, the engine will continue to run in the background.
     */
    fun cacheEngine(context: Context): NssBuilder<T> {
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