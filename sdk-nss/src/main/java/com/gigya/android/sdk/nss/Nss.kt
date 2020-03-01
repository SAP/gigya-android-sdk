package com.gigya.android.sdk.nss

import android.content.Context
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.nss.engine.NssEngineCoordinator
import com.gigya.android.sdk.nss.utils.guard
import org.json.JSONObject
import java.io.IOException

class Nss private constructor(
        private val assetPath: String?,
        private val initialRoute: String?,
        private val events: NssEvents?) : NssEngineCoordinator {

    companion object {

        const val LOG_TAG = "NssBuilder"
    }

    init {
        initializeEngine()
    }

    data class Builder(
            var assetPath: String? = null,
            var initialRoute: String? = null,
            var events: NssEvents? = null) {

        fun assetPath(assetPath: String) = apply { this.assetPath = assetPath }
        fun initialRoute(initialRoute: String) = apply { this.initialRoute = initialRoute }
        fun events(events: NssEvents) = apply {
            this.events = events
            this.events?.let {
                // Injecting the events callback to the singleton view model.
                val viewModel = GigyaNss.dependenciesContainer.get(NssViewModel::class.java)
                viewModel.mEvent = events
            }
        }

        fun show(launcherContext: Context) = Nss(assetPath, initialRoute, events)
                .show(launcherContext)
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
            var jsonAsset = loadJsonFromAssets(launcherContext, assetPath)
            jsonAsset.guard {
                GigyaLogger.error(LOG_TAG, "Failed to parse JSON asset")
                throw RuntimeException("Failed to parse JSON File from assets folder")
            }
            initialRoute?.let {
                // CAMBINA.
                jsonAsset = JSONObject(jsonAsset).put("initialRoute", it).toString()
            }

            NssActivity.start(
                    launcherContext,
                    markup = jsonAsset!!)

        } ?: applyError("Asset path not available")
    }

    /**
     * Notify error using available result handler.
     */
    @Suppress("SameParameterValue")
    private fun applyError(cause: String) = events?.onException(cause)

    interface ResultHandler {

        fun onError(cause: String)
    }

}