package com.gigya.android.sdk.nss

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.transition.Slide
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.channel.IgnitionMethodChannel
import com.gigya.android.sdk.nss.engine.NssEngineLifeCycle
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refine

class NssActivity<T : GigyaAccount> : FragmentActivity() {

    private var mViewModel: NssFlowViewModel<T>? = null

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

        engineLifeCycle = GigyaNss.dependenciesContainer.get(NssEngineLifeCycle::class.java)

        val markup = intent?.extras?.getSerializable(EXTRA_MARKUP)
        markup.guard {
            throw RuntimeException("Missing markup. Please provide markup on activity instantiation")
        }

        val engine = engineLifeCycle?.getNssEngine()
        engine.guard {
            throw RuntimeException("NSS engine failed to initialize!")
        }

        GigyaNss.dependenciesContainer.get(NssFlowViewModel::class.java).refine<NssFlowViewModel<T>> {
            mViewModel = this
            mViewModel!!.mFinish = {
                onFinishReceived()
            }
        }

        // Load channels.
        mViewModel?.loadChannels(engine!!)

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
        val ignitionChannel = GigyaNss.dependenciesContainer.get(IgnitionMethodChannel::class.java)
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
     * Engine notified that the main flow/work has ended.
     * Dismiss/destroy the activity.
     */
    private fun onFinishReceived() {
        mViewModel?.dispose()
        mViewModel = null
        finish()
    }

    override fun onDestroy() {
        engineLifeCycle?.disposeEngine()
        super.onDestroy()
    }

}