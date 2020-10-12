package com.gigya.android.sdk.nss

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.transition.Slide
import com.gigya.android.sdk.Config
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.channel.IgnitionMethodChannel
import com.gigya.android.sdk.nss.channel.IgnitionMethodChannel.IgnitionCall
import com.gigya.android.sdk.nss.engine.NssEngineLifeCycle
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refine
import com.gigya.android.sdk.utils.UiUtils

/**
 * Nss main activity.
 * To assure correct markup injection flow, the activity will initiate the Flutter engine
 * within a FlutterFragment.
 */
class NssActivity<T : GigyaAccount> : androidx.fragment.app.FragmentActivity() {

    private var viewModel: NssViewModel<T>? = null
    private var isDisplayed = false
    private var engineLifeCycle: NssEngineLifeCycle? = null

    companion object {

        const val LOG_TAG = "NativeScreenSetsActivity"
        const val FRAGMENT_ENTER_ANIMATION_DURATION = 450L
        private const val EXTRA_MARKUP = "extra_markup"

        fun start(context: Context, markup: Map<String, Any>?) {
            val intent: Intent = Intent(context, NssActivity::class.java)
            markup?.let { map ->
                intent.putExtra(EXTRA_MARKUP, HashMap(map))
            }
            context.startActivity(intent)
        }
    }

    private fun secureIfNeeded() {
        try {
            val secureActivity = Gigya.getContainer().get(Config::class.java).isSecureActivities
            if (secureActivity) {
                // Apply Secure flag.
                UiUtils.secureActivity(window)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        secureIfNeeded()
        setContentView(R.layout.nss_activity)

        engineLifeCycle = Gigya.getContainer().get(NssEngineLifeCycle::class.java)

        val markup = intent?.extras?.getSerializable(EXTRA_MARKUP)
        markup.guard {
            throw RuntimeException("Missing markup. Please provide markup on activity instantiation")
        }

        val engine = engineLifeCycle?.getNssEngine()
        engine.guard {
            throw RuntimeException("NSS engine failed to initialize!")
        }

        Gigya.getContainer().get(NssViewModel::class.java).refine<NssViewModel<T>> {
            viewModel = this
            viewModel!!.finishClosure = {
                onFinishReceived()
            }
            viewModel!!.intentAction = { intent ->
                startActivity(intent)
            }
            viewModel!!.intentActionForResult = { intent, requestCode ->
                startActivityForResult(intent, requestCode)
            }
        }

        // Load channels.
        viewModel?.loadChannels(engine!!)

        GigyaLogger.debug(LOG_TAG, "Registered nss method channels.")

        val fragment = engineLifeCycle?.getEngineFragment()
        fragment.guard {
            throw RuntimeException("Failed to initialize flutter fragment")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val slide = Slide()
            slide.duration = FRAGMENT_ENTER_ANIMATION_DURATION
            fragment!!.enterTransition = slide
        }

        // Register ignition channel.
        val ignitionChannel = Gigya.getContainer().get(IgnitionMethodChannel::class.java)
        ignitionChannel.initChannel(engine!!.dartExecutor.binaryMessenger)
        ignitionChannel.flutterMethodChannel?.setMethodCallHandler { call, result ->
            GigyaLogger.debug(LOG_TAG, "Ignition channel call ${call.method}")
            when (call.method) {
                IgnitionCall.IGNITION.identifier -> {
                    result.success(markup)
                }
                IgnitionCall.READY_FOR_DISPLAY.identifier -> {
                    if (!isDisplayed) {

                    }
                }
                IgnitionCall.SCHEMA.identifier -> {
                    viewModel?.loadSchema(result)
                }
            }
        }

        engineLifeCycle?.engineExecuteMain()

        supportFragmentManager.beginTransaction()
                .replace(R.id.nss_main_frame, fragment!!)
                .commit()
        isDisplayed = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_CANCELED -> {
                viewModel?.cancelImageRequest()
            }
            Activity.RESULT_OK -> {
                if (requestCode == 1666) {
                    // Resolve image selection from available image Uri.
                    data?.data?.let { uri ->
                        viewModel?.handleDynamicImageUri(uri)
                        return
                    }
                    // Resolve image capture from camera using bitmap data.
                    data?.extras!!["data"]?.let { bitmap ->
                        viewModel?.handleDynamicImageBitmap(bitmap as Bitmap)
                        return
                    }
                }
            }
        }
    }

    /**
     * Engine notified that the main flow/work has ended.
     * Dismiss/destroy the activity.
     */
    private fun onFinishReceived() {
        viewModel?.dispose()
        viewModel = null
        finish()
    }

    override fun onDestroy() {
        // Make sure we dispose the engine on activity destroy.
        engineLifeCycle?.disposeEngine()

        super.onDestroy()
    }
}