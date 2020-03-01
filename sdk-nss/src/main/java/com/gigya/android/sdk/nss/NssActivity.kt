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
import com.gigya.android.sdk.nss.engine.NssEngineCoordinator
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refine
import java.util.*

class NssActivity<T : GigyaAccount> : FragmentActivity(), NssEngineCoordinator {

    private var mViewModel: NssViewModel<T>? = null

    companion object {

        const val LOG_TAG = "NativeScreenSetsActivity"

        const val FRAGMENT_ENTER_ANIMATION_DURATION = 450L

        // Extras.
        private const val EXTRA_INITIAL_ROUTE = "extra_initial_route"
        private const val EXTRA_MARKUP = "extra_markup"

        fun start(context: Context, markup: String) {
            val intent: Intent = Intent(context, NssActivity::class.java)
            intent.putExtra(EXTRA_MARKUP, markup)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.nss_activity)

        val markup = intent?.extras?.getString(EXTRA_MARKUP)
        markup.guard {
            throw RuntimeException("Missing markup. Please provide markup on activity instantiation")
        }

        val engine = getNssEngine()
        engine.guard {
            throw RuntimeException("NSS engine failed to initialize!")
        }

        GigyaNss.dependenciesContainer.get(NssViewModel::class.java).refine<NssViewModel<T>> {
            mViewModel = this
            mViewModel!!.mMarkup = markup
            mViewModel!!.mFinish = {
                onFinishReceived()
            }
        }

        // Load channels.
        mViewModel?.loadChannels(engine!!)

        GigyaLogger.debug(LOG_TAG, "Registered nss method channels.")

        val fragment = getEngineFragment()
        fragment.guard {
            throw RuntimeException("Failed to initialize flutter fragment")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val slide = Slide()
            slide.duration = FRAGMENT_ENTER_ANIMATION_DURATION
            fragment.enterTransition = slide
        }

        // Register ignition channel.
        val ignitionChannel = GigyaNss.dependenciesContainer.get(IgnitionMethodChannel::class.java)
        ignitionChannel.initChannel(engine!!.dartExecutor.binaryMessenger)
        ignitionChannel.flutterMethodChannel?.setMethodCallHandler { call, result ->
            GigyaLogger.debug(LOG_TAG, "Ignition channel call ${call.method}")
            when (call.method) {
                IgnitionCall.IGNITION.lowerCase() -> {
                    result.success(markup)
                }
                IgnitionCall.READY_FOR_DISPLAY.lowerCase() -> {
                    supportFragmentManager.beginTransaction()
                            .add(R.id.nss_main_frame, fragment)
                            .commit()
                }
            }
        }

        engineExecuteMain()
    }

    /**
     * Engine notified that the main flow/work has ended.
     * Dismiss/destroy the activity.
     */
    private fun onFinishReceived() {
        mViewModel?.dispose()
        finish()
    }

    override fun onDestroy() {
        disposeEngine();
        super.onDestroy()
    }

    internal enum class IgnitionCall {
        IGNITION, READY_FOR_DISPLAY;

        fun lowerCase() = this.name.toLowerCase(Locale.ENGLISH)
    }

}