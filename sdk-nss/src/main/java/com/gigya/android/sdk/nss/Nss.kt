package com.gigya.android.sdk.nss

import android.content.Context
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.engine.NssEngineLifeCycle
import com.gigya.android.sdk.nss.utils.NssJsonDeserializer
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import com.gigya.android.sdk.nss.utils.serialize
import com.gigya.android.sdk.utils.FileUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.IOException

class Nss private constructor(
        private val engineLifeCycle: NssEngineLifeCycle,
        private val assetPath: String?,
        private val initialRoute: String?,
        private val events: NssEvents<*>?) {

    private val gson: Gson = GsonBuilder().registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, NssJsonDeserializer()).create()

    companion object {

        const val LOG_TAG = "NssBuilder"
    }

    init {
        engineLifeCycle.initializeEngine()
    }

    data class Builder(
            var assetPath: String? = null,
            var initialRoute: String? = null,
            var events: NssEvents<*>? = null) {

        fun assetPath(assetPath: String) = apply { this.assetPath = assetPath }
        fun initialRoute(initialRoute: String) = apply { this.initialRoute = initialRoute }
        fun <T : GigyaAccount> events(events: NssEvents<T>) = apply {
            this.events = events
            this.events?.let {
                // Injecting the events callback to the singleton view model.
                Gigya.getContainer().get(NssFlowViewModel::class.java).refined<NssFlowViewModel<T>> { viewModel ->
                    viewModel.nssEvents = events
                }
            }
        }

        fun show(launcherContext: Context) = Nss(
                Gigya.getContainer().get(NssEngineLifeCycle::class.java),
                assetPath,
                initialRoute,
                events)
                .show(launcherContext)
    }

    /**
     * Load markup file from assets folder given filename/path.
     */
    private fun loadJsonFromAssets(context: Context, fileName: String) = try {
        GigyaLogger.debug(LOG_TAG, "loadJsonFromAssets() with fileName $fileName")
        FileUtils.assetJsonFileToString(context, fileName)
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        null
    }

    /**
     * Show native screensets.
     */
    fun show(launcherContext: Context) {
        assetPath?.apply {
            val jsonAsset = loadJsonFromAssets(launcherContext, assetPath)
            jsonAsset.guard {
                GigyaLogger.error(LOG_TAG, "Failed to parse JSON asset")
                throw RuntimeException("Failed to parse JSON File from assets folder")
            }

            engineLifeCycle.show(launcherContext, mapAsset(jsonAsset!!))

        } ?: throw RuntimeException("Asset path not available")
    }

    private fun mapAsset(jsonAsset: String): Map<String, Any> {
        val jsonMap = jsonAsset.serialize<String, Any>(gson)
        jsonMap.guard {
            throw RuntimeException("Markup parsing error")
        }
                .refined<MutableMap<String, Any>> { map ->
                    @Suppress("UNCHECKED_CAST") val routingMap: MutableMap<String, Any> = map["routing"] as MutableMap<String, Any>
                    initialRoute?.let { userDefinedInitialRoute ->
                        routingMap["initial"] = userDefinedInitialRoute
                    }
                    if (!routingMap.containsKey("initial")) {
                        throw  RuntimeException("Markup scheme incorrect - initial route must be provided")
                    }
                }
        return jsonMap
    }
}