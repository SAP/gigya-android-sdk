package com.gigya.android.sdk.nss

import android.content.Context
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.nss.utils.guard
import java.io.IOException

class Nss private constructor(
        private val assetPath: String?,
        private val initialRoute: String?,
        private val handler: ResultHandler?) {

    companion object {

        const val LOG_TAG = "NssBuilder"
    }

    interface ResultHandler {

        fun onError(cause: String)
    }

    data class Builder(
            var assetPath: String? = null,
            var initialRoute: String? = null,
            var handler: ResultHandler? = null) {

        fun assetPath(assetPath: String) = apply { this.assetPath = assetPath }
        fun initialRoute(initialRoute: String) = apply { this.initialRoute = initialRoute }
        fun handler(handler: ResultHandler) = apply { this.handler = handler }
        fun show(launcherContext: Context) = Nss(assetPath, initialRoute, handler).show(launcherContext)
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
     * Show screensets.
     */
    fun show(launcherContext: Context) {
        assetPath?.apply {
            val jsonAsset = loadJsonFromAssets(launcherContext, assetPath)
            jsonAsset.guard {
                GigyaLogger.error(LOG_TAG, "Failed to parse JSON asset")
                throw RuntimeException("Failed to parse JSON File from assets folder")
            }

            // Start NssActivity using assets provided markup.
            NssActivity.start(
                    launcherContext,
                    markup = jsonAsset!!,
                    initialRoute = initialRoute)
        } ?: applyError("Asset path not available")
    }

    /**
     * Notify error using available result handler.
     */
    @Suppress("SameParameterValue")
    private fun applyError(cause: String) = handler?.onError(cause)


}