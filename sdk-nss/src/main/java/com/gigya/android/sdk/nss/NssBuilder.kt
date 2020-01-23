package com.gigya.android.sdk.nss

import android.content.Context
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import java.io.IOException

data class NssBuilder(var assetPath: String? = null,
                      var resultHandler: ResultHandler? = null) {

    private var flutterEngine: FlutterEngine? = null

    interface ResultHandler {

        fun onError(cause: String)
    }

    private fun loadJsonFromAssets(context: Context, path: String) = try {
        context.assets.open(path).bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        null
    }

    fun cacheEngine(context: Context): NssBuilder {
        // Creating a new instance of the Flutter engine to be cached.
        flutterEngine = FlutterEngine(context)

        // Cache engine instance.
        FlutterEngineCache
                .getInstance()
                .put(GigyaNss.FLUTTER_ENGINE_ID, flutterEngine)
        return this
    }

    fun show(launcherContext: Context, initialRoute: String, handler: ResultHandler) {
        resultHandler = handler
        // Try to load from assets as a the default action.
        assetPath?.apply {
            val jsonAsset = loadJsonFromAssets(launcherContext, assetPath!!)
            jsonAsset?.apply {
                NativeScreenSetsActivity.start(
                        launcherContext,
                        flutterEngine != null,
                        markup = jsonAsset,
                        initialRoute = initialRoute)
            } ?: applyError("Failed to load provided asset")
        } ?: applyError("Asset path not available")
    }

    private fun applyError(cause: String) = resultHandler?.onError(cause)


}