package com.gigya.android.sdk.nss

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.transition.Slide
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
        private const val EXTRA_DATA = "extra_data"

        fun start(context: Context, data: IgnitionData) {
            val intent: Intent = Intent(context, NssActivity::class.java)
            intent.putExtra(EXTRA_DATA, data)
            context.startActivity(intent)
        }
    }

    // Custom result handler for FIDO sender intents.
    private val webAuthnResultHandler: ActivityResultLauncher<IntentSenderRequest> =
        registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { activityResult ->
            val extras =
                activityResult.data?.extras?.keySet()?.map { "$it: ${intent.extras?.get(it)}" }
                    ?.joinToString { it }
            Gigya.getInstance().WebAuthn().handleFidoResult(activityResult)
        }

    /**
     * Add FLAG_SECURE to activity if specified in the Gigya interface using "secureActivityWindow" method.
     */
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

        val ignitionData = intent?.extras?.getParcelable<IgnitionData>(EXTRA_DATA)
        ignitionData.guard {
            throw RuntimeException("Missing initialization data. Please verify that at least on the the NSS loading options has been provided (asset, hosted id).")
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
            viewModel!!.attachWebAuthnResultHandler(webAuthnResultHandler)
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
                    // Load markup.
                    viewModel?.loadMarkup(ignitionData!!,
                        done = { markup ->
                            markup?.let {
                                result.success(markup)
                            }
                        },
                        error = { error ->
                            viewModel?.nssEvents?.onError("", error)
                            onBackPressed()
                        })
                        ?: GigyaLogger.error(
                            LOG_TAG,
                            "Markup not available. Please check paths (asset or hosted)"
                        );
                }
                IgnitionCall.STYLES.identifier -> {
                    // Load styles.
                    viewModel?.loadStyles(
                        done = { styles ->
                            result.success(styles)
                        },
                        error = { error ->
                            viewModel?.nssEvents?.onError("", error)
                            onBackPressed()
                        }
                    )
                }

                IgnitionCall.READY_FOR_DISPLAY.identifier -> {
                    if (!isDisplayed) {
                        // A short transformation is required to avoid engine first load Jitter.
                        isDisplayed = true
                        applyProgressTransform()
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
    }

    private fun applyProgressTransform() {
        val mainFrame = findViewById<View>(R.id.nss_main_frame)
        val loadingView = findViewById<View>(R.id.nss_progress_frame)

        val duration = resources.getInteger(
            android.R.integer.config_mediumAnimTime
        )

        mainFrame.animate()
            .alpha(1f)
            .setDuration(duration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    mainFrame.visibility = View.VISIBLE
                }
            })

        loadingView.animate()
            .alpha(0f)
            .setDuration(duration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    loadingView.visibility = View.GONE
                }
            })
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

    override fun onBackPressed() {
        viewModel?.onBackPressed()
    }
}