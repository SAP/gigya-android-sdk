package com.gigya.android.sdk.nss

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.bloc.SchemaHelper
import com.gigya.android.sdk.nss.bloc.action.NssSetAccountAction
import com.gigya.android.sdk.nss.bloc.data.NssDataResolver
import com.gigya.android.sdk.nss.bloc.data.NssJsEvaluator
import com.gigya.android.sdk.nss.bloc.events.FieldEventModel
import com.gigya.android.sdk.nss.bloc.events.NssScreenEvents
import com.gigya.android.sdk.nss.bloc.events.ScreenEventsManager
import com.gigya.android.sdk.nss.bloc.events.ScreenEventsModel
import com.gigya.android.sdk.nss.bloc.flow.NssFlowManager
import com.gigya.android.sdk.nss.channel.*
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import com.gigya.android.sdk.reporting.ReportingManager
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import java.util.logging.StreamHandler

class NssViewModel<T : GigyaAccount>(
    private val screenChannel: ScreenMethodChannel,
    private val dataChannel: DataMethodChannel,
    private val apiChannel: ApiMethodChannel,
    private val logChannel: LogMethodChannel,
    private val eventsChannel: EventsMethodChannel,
    private val flowManager: NssFlowManager<T>,
    private val schemaHelper: SchemaHelper<T>,
    private val nssDataResolver: NssDataResolver,
    private val screenEventsManager: ScreenEventsManager,
    private val nssMarkupLoader: NssMarkupLoader<T>,
    private val nssJSEvaluator: NssJsEvaluator,
) {

    // Default language will be set to English unless specified by initiator.
    private var screenLanguage: String = "en"

    var finishClosure: () -> Unit? = { }

    lateinit var intentAction: (Intent) -> Unit?
    lateinit var intentActionForResult: (Intent, Int) -> Unit?

    lateinit var backInterceptorChannel: EventChannel
    var backInterceptorChannelSink: EventChannel.EventSink? = null

    fun attachWebAuthnResultHandler(handler: ActivityResultLauncher<IntentSenderRequest>) {
        flowManager.fidoResultHandler = handler
    }

    var nssEvents: NssEvents<T>? = null
        set(value) {
            field = value
            flowManager.nssEvents = value
        }

    companion object {

        const val LOG_TAG = "NssViewModel"
    }

    internal fun dispose() {
        nssEvents = null
        screenChannel.dispose()
        logChannel.dispose()
        apiChannel.dispose()
        screenEventsManager.dispose()
    }

    /**
     * Register Flutter engine specific communication channels used for Nss usage.
     */
    fun loadChannels(engine: FlutterEngine) {
        screenChannel.initChannel(engine.dartExecutor.binaryMessenger)
        screenChannel.setMethodChannelHandler(screenMethodChannelHandler)

        apiChannel.initChannel(engine.dartExecutor.binaryMessenger)
        apiChannel.setMethodChannelHandler(apiMethodChannelHandler)

        logChannel.initChannel(engine.dartExecutor.binaryMessenger)
        logChannel.setMethodChannelHandler(logMethodChannelHandler)

        dataChannel.initChannel(engine.dartExecutor.binaryMessenger)
        dataChannel.setMethodChannelHandler(dataMethodChannelHandler)

        eventsChannel.initChannel(engine.dartExecutor.binaryMessenger)
        eventsChannel.setMethodChannelHandler(eventsMethodChannelHandler)

        backInterceptorChannel =
            EventChannel(engine.dartExecutor.binaryMessenger, "nativeBackEventStream")
        backInterceptorChannel.setStreamHandler(object : EventChannel.StreamHandler {

            override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
                backInterceptorChannelSink = events
            }

            override fun onCancel(arguments: Any?) {
                // Redundant.
            }

        })
    }

    fun onBackPressed() {
        backInterceptorChannelSink?.success(null)
    }

    /**
     * Handle engine logging options.
     */
    private val logMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, _ ->
            call.arguments.refined<Map<String, String>> { logMap ->
                when (call.method) {
                    LogMethodChannel.LogCall.DEBUG.identifier -> {
                        GigyaLogger.debug(logMap["tag"], logMap["message"])
                    }

                    LogMethodChannel.LogCall.ERROR.identifier -> {
                        GigyaLogger.error(logMap["tag"], logMap["message"])
                        // Development errors logs from the engine will throw a runtime exception.
                        throw RuntimeException(logMap["message"])
                    }
                }
            }
        }
    }

    /**
     * Handle engine screen actions.
     */
    private val screenMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, result ->
            when (call.method) {
                // Screen initiated with action call.
                ScreenMethodChannel.ScreenCall.ACTION.identifier -> {
                    GigyaLogger.debug(LOG_TAG, "screen action")
                    call.arguments.refined<Map<String, String>> { map ->
                        val actionId = map["actionId"]
                        val screenId = map["screenId"]
                        val expressions = map["expressions"] as Map<String, String>

                        actionId.guard {
                            GigyaLogger.error(
                                LOG_TAG,
                                "Missing action if in screen action initializer"
                            )
                            ReportingManager.get().error(
                                GigyaNss.VERSION,
                                "nss",
                                "Missing action if in screen action initializer"
                            )
                        }
                        flowManager.setCurrent(actionId!!, screenId!!, expressions, result)
                    }
                }
                // Screen initiated with dismiss call.
                ScreenMethodChannel.ScreenCall.DISMISS.identifier -> {
                    GigyaLogger.debug(LOG_TAG, "screen dismiss")
                    flowManager.dispose()
                    finishClosure()
                }
                // Screen initiated with cancel call.
                ScreenMethodChannel.ScreenCall.CANCEL.identifier -> {
                    GigyaLogger.debug(LOG_TAG, "screen action")
                    // Pass a cancel event to main Nss events interface.
                    nssEvents?.onCancel()
                    flowManager.dispose()
                    finishClosure()
                }
                // Screen initiated an external link call.
                ScreenMethodChannel.ScreenCall.LINK.identifier -> {
                    GigyaLogger.debug(LOG_TAG, "screen link")
                    call.arguments.refined<Map<String, String>> { map ->
                        val uri = Uri.parse(map["url"])
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        // Forward intent action to activity in order to open browser URL.
                        intentAction(intent)
                    }
                }
                // Screen request to evaluate expression by demand.
                ScreenMethodChannel.ScreenCall.EVAL.identifier -> {
                    GigyaLogger.debug(LOG_TAG, "screen eval")
                    call.arguments.refined<Map<String, String>> { map ->
                        var expression: String? = null
                        if (map["expression"] != null) {
                            expression = map["expression"] as String
                        }
                        val globalData =
                            flowManager.activeAction?.getGlobalData() as Map<String, Any>
                        val data = map["data"] as Map<String, Any>
                        nssJSEvaluator.evalSingle(data + globalData, expression!!) { eval ->
                            result.success(eval)
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle specific engine component data requests.
     */
    private val dataMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, result ->
            when (call.method) {
                "image_resource" -> {
                    call.arguments.refined<MutableMap<String, Any>> { args ->
                        nssDataResolver.fetchImageResource(args, result)
                    }
                }

                "pick_image" -> {
                    flowManager.activeAction.refined<NssSetAccountAction<T>> {
                        // Set reference to active data result.
                        it.profileImageResult = result
                    }
                    // Need to save a reference to the result in order to correctly process
                    // the activity result for this action.
                    intentActionForResult(nssDataResolver.imageSelectionIntent(), 1666)
                }
            }
        }
    }

    /**
     * Handle engine custom event requests.
     */
    private val eventsMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, result ->
            val sid = call.argument<String>("sid")
            if (sid != null) {
                val events: NssScreenEvents? = screenEventsManager.eventsFor(sid)
                if (events != null) {
                    val screenModel = ScreenEventsModel()
                    screenModel.engineResponse = result
                    screenModel.data = call.argument("data") ?: mutableMapOf()
                    when (call.method) {
                        "screenDidLoad" -> {
                            events.screenDidLoad()
                            result.success(null)
                        }

                        "routeFrom" -> {
                            screenModel.pr = call.argument("pid") ?: ""
                            events.routeFrom(screenModel)
                        }

                        "routeTo" -> {
                            screenModel.nr = call.argument("nid") ?: ""
                            events.routeTo(screenModel)
                        }

                        "submit" -> {
                            events.submit(screenModel)
                        }

                        "fieldDidChange" -> {
                            val fieldModel = FieldEventModel(
                                screenModel.data["field"] as String,
                                screenModel.data["from"] as String?,
                                screenModel.data["to"] as String?
                            )
                            events.fieldDidChange(screenModel, fieldModel)
                        }
                    }
                    return@MethodCallHandler
                }
                screenEventsManager.disposeResult(result)
                return@MethodCallHandler
            }
            screenEventsManager.disposeResult(result)
        }
    }

    /**
     * Load the markup provided from NSS builder.
     */
    fun loadMarkup(
        data: IgnitionData,
        done: (Map<String, Any>?) -> Unit,
        error: (GigyaError) -> Unit
    ) {
        data.lang?.let { language ->
            screenLanguage = language
        }
        // In any case, populate language with default "en" if not specified in ignition data.
        data.lang = screenLanguage
        nssMarkupLoader.loadMarkupFrom(data,
            markupLoaded = { markup ->
                done(markup)
            },
            markupFailedToLoad = { e ->
                error(e)
            })
    }

    fun loadStyles(
        done: (Map<String, Any>?) -> Unit,
        error: (GigyaError) -> Unit
    ) {
        nssMarkupLoader.getStyleLibraryData(
            onStylesLoaded = { styles ->
                done(styles)
            },
            onLoadError = { error ->
                error(error)
            }
        )
    }

    /**
     * Cancel event triggered from image selection flow.
     */
    fun cancelImageRequest() {
        flowManager.activeAction.refined<NssSetAccountAction<T>> {
            // Set reference to active data result.
            it.profileImageResult?.success(null)
        }
    }

    /**
     * Handle engine image request request from Uri.
     */
    fun handleDynamicImageUri(uri: Uri) {
        val data = nssDataResolver.getBitmapDataFromUri(uri)
        data?.let {
            flowManager.activeAction?.onNext(
                NssSetAccountAction.setProfilePhoto,
                mutableMapOf("data" to data)
            )
        }
    }

    /**
     * Handle engine image request from bitmap.
     */
    fun handleDynamicImageBitmap(bitmap: Bitmap) {
        val data = nssDataResolver.getBitmapData(bitmap)
        data?.let {
            flowManager.activeAction?.onNext(
                NssSetAccountAction.setProfilePhoto,
                mutableMapOf("data" to data)
            )
        }
    }

    /**
     * Handle engine API requests.
     */
    private val apiMethodChannelHandler: MethodChannel.MethodCallHandler by lazy {
        MethodChannel.MethodCallHandler { call, result ->
            call.arguments.refined<MutableMap<String, Any>> { args ->
                // Add language parameter to all api call.
                // English will be added by default unless language is
                // specified in ignition data.
                args["lang"] = screenLanguage
                flowManager.onNext(call.method, args, result)
            }
        }
    }

    /**
     * Load site schema.
     */
    fun loadSchema(result: MethodChannel.Result) {
        schemaHelper.getSchema(result)
    }

    /**
     * Manually dismiss the engine.
     */
    fun nssDismiss() {
        GigyaLogger.debug(LOG_TAG, "screen dismiss")
        flowManager.dispose()
        finishClosure()
    }

}