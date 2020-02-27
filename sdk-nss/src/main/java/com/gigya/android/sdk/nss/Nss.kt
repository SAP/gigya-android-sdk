package com.gigya.android.sdk.nss

import android.content.Context
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.nss.channel.IgnitionMethodChannel
import com.gigya.android.sdk.nss.utils.guard
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.view.FlutterMain
import org.json.JSONObject
import java.io.IOException
import java.util.*

class Nss private constructor(
        private val assetPath: String?,
        private val initialRoute: String?,
        private val events: NssEvents?) {

    companion object {

        const val LOG_TAG = "NssBuilder"
    }

    data class Builder(
            var assetPath: String? = null,
            var initialRoute: String? = null,
            var events: NssEvents? = null) {

        init {
            if (!FlutterEngineCache
                            .getInstance().contains(GigyaNss.FLUTTER_ENGINE_ID)) {
                val engine = FlutterEngine(GigyaNss.dependenciesContainer.get(Context::class.java))
                val ignitionChannel = GigyaNss.dependenciesContainer.get(IgnitionMethodChannel::class.java)
                ignitionChannel.initChannel(engine.dartExecutor.binaryMessenger)
                FlutterEngineCache.getInstance().put(GigyaNss.FLUTTER_ENGINE_ID, engine)
            }
        }

        fun assetPath(assetPath: String) = apply { this.assetPath = assetPath }
        fun initialRoute(initialRoute: String) = apply { this.initialRoute = initialRoute }
        fun events(events: NssEvents) = apply {
            this.events = events
            this.events?.let {
                // Injecting the events callback to the singleton view model.
                val viewModel = GigyaNss.dependenciesContainer.get(NssViewModel::class.java)
                viewModel.events = events
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

            val mainChannel = GigyaNss.dependenciesContainer.get(IgnitionMethodChannel::class.java)
            mainChannel.flutterMethodChannel?.setMethodCallHandler { call, result ->
                when (call.method) {
                    MainCall.IGNITION.lowerCase() -> {
                        initialRoute?.let {
                            jsonAsset = JSONObject(jsonAsset).put("initialRoute", it).toString()
                        }
                        result.success(jsonAsset)
                    }
                    MainCall.READY_FOR_DISPLAY.lowerCase() -> {
                        NssActivity.start(
                                launcherContext,
                                markup = jsonAsset!!,
                                initialRoute = initialRoute)
                    }
                }
            }

            FlutterEngineCache.getInstance().get(GigyaNss.FLUTTER_ENGINE_ID)
                    ?.dartExecutor?.executeDartEntrypoint(DartExecutor.DartEntrypoint(
                    FlutterMain.findAppBundlePath(),
                    "main")
            )
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

    internal enum class MainCall {
        IGNITION, READY_FOR_DISPLAY;

        fun lowerCase() = this.name.toLowerCase(Locale.ENGLISH)
    }

}