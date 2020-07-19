package com.gigya.android.sdk.nss

import android.content.Context
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.engine.NssEngineLifeCycle
import com.gigya.android.sdk.nss.utils.*
import com.gigya.android.sdk.utils.FileUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.IOException

/**
 * Main NSS builder class.
 * Starting up the Nss engine requires specific parameters and will throw a RuntimeException in the
 * event of:
 * 1 - Malformed JSON markup.
 * 2 - Missing initialRoute parameter.
 */
class Nss private constructor(
        private val engineLifeCycle: NssEngineLifeCycle,
        private val assetPath: String?,
        private val initialRoute: String?,
        private val lang: String? = "_default",
        private val events: NssEvents<*>?) {

    private val gson: Gson = GsonBuilder()
            .registerTypeAdapter(object : TypeToken<Map<String, Any>>() {}.type, NssJsonDeserializer()).create()

    companion object {

        const val LOG_TAG = "NssBuilder"
        const val THEME_SUFFIX = ".theme.json"
        const val LOCALIZATION_SUFFIX = ".i18n.json"
    }

    init {
        engineLifeCycle.initializeEngine()
    }

    data class Builder(
            var assetPath: String? = null,
            var initialRoute: String? = null,
            var lang: String? = null,
            var events: NssEvents<*>? = null) {

        fun assetPath(assetPath: String) = apply { this.assetPath = assetPath }
        fun initialRoute(initialRoute: String) = apply { this.initialRoute = initialRoute }
        fun lang(language: String) = apply { this.lang = language }
        fun <T : GigyaAccount> events(events: NssEvents<T>) = apply {
            this.events = events
            this.events?.let {
                // Injecting the events callback to the singleton view model.
                Gigya.getContainer().get(NssViewModel::class.java).refined<NssViewModel<T>> { viewModel ->
                    viewModel.nssEvents = events
                }
            }
        }

        fun show(launcherContext: Context) = Nss(
                Gigya.getContainer().get(NssEngineLifeCycle::class.java),
                assetPath,
                initialRoute,
                lang,
                events)
                .show(launcherContext)
    }

    /**
     * Load markup file from assets folder given filename/path.
     * @param context Application/Available context.
     * @param fileName Asset file name.
     */
    private fun loadJsonFromAssets(context: Context, fileName: String) = try {
        GigyaLogger.debug(LOG_TAG, "loadJsonFromAssets() with fileName $fileName.json")
        FileUtils.assetJsonFileToString(context, fileName)
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        null
    }

    /**
     * Show native screen-sets.
     * @param launcherContext Root activity context.
     */
    fun show(launcherContext: Context) {
        assetPath?.apply {
            val jsonAsset = loadJsonFromAssets(launcherContext, "$assetPath.json")
            jsonAsset.guard {
                GigyaLogger.error(LOG_TAG, "Failed to parse JSON asset")
                throw RuntimeException("Failed to parse JSON File from assets folder")
            }

            // Load asset theme file.
            val themeAsset = loadJsonFromAssets(launcherContext, "$assetPath$THEME_SUFFIX")
            val localizationAsset = loadJsonFromAssets(launcherContext, "$assetPath$LOCALIZATION_SUFFIX")

            engineLifeCycle.show(launcherContext, mapAsset(jsonAsset!!, themeAsset, localizationAsset))

        } ?: throw RuntimeException("Asset path not available")
    }

    /**
     * Map JSON assets.
     * @param jsonAsset Main JSON markup asset.
     * @param themeAsset Optional theme markup asset.
     */
    private fun mapAsset(jsonAsset: String, themeAsset: String? = null, localizationAsset: String? = null): Map<String, Any> {
        val jsonMap = gson.fromJson<Map<String, Any>>(jsonAsset, object: TypeToken<Map<String, Any>>() {}.type)
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
                    // Add optional theme map.
                    themeAsset?.let {
                        val themeMap = it.serialize<String, Any>(gson)
                        GigyaLogger.debug(LOG_TAG, "Adding parsed theme map to JSON markup")
                        map["theme"] = themeMap
                    }

                    // Add optional localization map.
                    localizationAsset?.let {
                        val localMap = it.serialize<String, Any>(gson)
                        GigyaLogger.debug(LOG_TAG, "Adding parsed localization map to JSON markup")
                        map["i18n"] = localMap
                        // Add default language is relevant only when an additional localization JSON map as been provided.
                        lang?.let { localizationLanguage ->
                            map["lang"] = localizationLanguage
                        }
                    }
                }
        return jsonMap
    }
}