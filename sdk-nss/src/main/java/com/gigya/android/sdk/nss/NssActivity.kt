package com.gigya.android.sdk.nss

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.transition.Slide
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.channel.IgnitionMethodChannel
import com.gigya.android.sdk.nss.engine.NssEngineLifeCycle
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refine

/**
 * Nss main activity.
 * To assure correct markup injection flow, the activity will initiate the Flutter engine
 * within a FlutterFragment.
 */
class NssActivity<T : GigyaAccount> : FragmentActivity() {

    private var viewModel: NssViewModel<T>? = null

    private var isDisplayed = false

    private var engineLifeCycle: NssEngineLifeCycle? = null

    companion object {

        const val LOG_TAG = "NativeScreenSetsActivity"

        const val FRAGMENT_ENTER_ANIMATION_DURATION = 450L

        // Extras.
        private const val EXTRA_MARKUP = "extra_markup"

        fun start(context: Context, markup: Map<String, Any>?) {
            val intent: Intent = Intent(context, NssActivity::class.java)
            markup?.let { map ->
                intent.putExtra(EXTRA_MARKUP, HashMap(map))
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            viewModel!!.intentAction = {
                onIntentAction(it)
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
                IgnitionMethodChannel.IgnitionCall.IGNITION.identifier -> {
                    result.success(markup)
                }
                IgnitionMethodChannel.IgnitionCall.READY_FOR_DISPLAY.identifier -> {
                    if (!isDisplayed) {
                        supportFragmentManager.beginTransaction()
                                .replace(R.id.nss_main_frame, fragment!!)
                                .commit()
                        isDisplayed = true
                    }
                }
            }
        }

        engineLifeCycle?.engineExecuteMain()
    }

    /**
     * Handle specific intent actions requested by the engine.
     */
    private fun onIntentAction(intent: Intent) {
        startActivity(intent)
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
        engineLifeCycle?.disposeEngine()
        super.onDestroy()
    }

}