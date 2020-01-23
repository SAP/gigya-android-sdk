package com.gigya.android.sdk.nss

import android.content.Context
import java.io.IOException

data class NssBuilder(var assetPath: String? = null,
                      var resultHandler: ResultHandler? = null) {

    interface ResultHandler {

        fun onError(cause: String)
    }

    private fun loadJsonFromAssets(context: Context, path: String) = try {
        context.assets.open(path).bufferedReader().use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        null
    }

    fun show(launcherContext: Context, initialRoute: String, handler: ResultHandler) {
        resultHandler = handler
        // Try to load from assets as a the default action.
        assetPath?.apply {
            val jsonAsset = loadJsonFromAssets(launcherContext, assetPath!!)
            jsonAsset?.apply {
                NativeScreenSetsActivity.start(
                        launcherContext,
                        markup = jsonAsset,
                        initialRoute = initialRoute)
            } ?: applyError("Failed to load provided asset")
        } ?: applyError("Asset path not available")
    }

    private fun applyError(cause: String) = resultHandler?.onError(cause)


}