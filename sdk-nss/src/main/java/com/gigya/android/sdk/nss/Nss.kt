package com.gigya.android.sdk.nss

import android.content.Context
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.nss.engine.NssEngineCoordinator
import com.gigya.android.sdk.nss.utils.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.io.IOException

class Nss private constructor(
        private val assetPath: String?,
        private val initialRoute: String?,
        private val events: NssEvents?) : NssEngineCoordinator {

    val gson: Gson = GsonBuilder().registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, NssJsonDeserializer()).create()

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
            val jsonAsset = loadJsonFromAssets(launcherContext, assetPath)
            jsonAsset.guard {
                GigyaLogger.error(LOG_TAG, "Failed to parse JSON asset")
                throw RuntimeException("Failed to parse JSON File from assets folder")
            }

            NssActivity.start(
                    launcherContext,
                    markup = mapAsset(jsonAsset!!))

        } ?: applyError("Asset path not available")
    }

    private fun mapAsset(jsonAsset: String) : Map<String, Any> {
        val jsonMap = jsonAsset.serialize<String, Any>(gson)
        jsonMap["markup"].guard {
            throw RuntimeException("Markup scheme incorrect - missing \"markup\" field")
        }
        jsonMap["markup"].refine<MutableMap<String, Any>> {
            initialRoute?.let {
                this.put("initialRoute", it)
            }
            if (!this.containsKey("initialRoute")) {
                throw  RuntimeException("Markup scheme incorrect - initial route must be provided")
            }
        }
        return jsonMap
    }

    /**
     * Notify error using available result handler.
     */
    @Suppress("SameParameterValue")
    private fun applyError(cause: String) = events?.onException(cause)
}