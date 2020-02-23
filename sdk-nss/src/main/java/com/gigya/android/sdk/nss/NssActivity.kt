package com.gigya.android.sdk.nss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.utils.guard
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.view.FlutterMain

class NssActivity<T : GigyaAccount> : FlutterActivity() {

    private lateinit var mViewModel: NssViewModel<T>

    companion object {

        const val LOG_TAG = "NativeScreenSetsActivity"

        // Extras.
        private const val EXTRA_INITIAL_ROUTE = "extra_initial_route"
        private const val EXTRA_MARKUP = "extra_markup"
        private const val EXTRA_CACHE_ENGINE = "extra_cache_engine"

        fun start(context: Context, withCachedEngine: Boolean = false, markup: String, initialRoute: String) {
            val intent: Intent = if (withCachedEngine) {
                NSSCachedEngineIntentBuilder().build(context)
            } else {
                NSSEngineIntentBuilder().build(context)
            }
            intent.putExtra(EXTRA_INITIAL_ROUTE, initialRoute)
            intent.putExtra(EXTRA_MARKUP, markup)
            intent.putExtra(EXTRA_CACHE_ENGINE, withCachedEngine)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val markup = intent?.extras?.getString(EXTRA_MARKUP)
        markup.guard {
            throw RuntimeException("Missing markup. Please provide markup on activity instantiation")
        }

        mViewModel = NssViewModelProviders.provideViewModel(markup!!, finish = { onFinishReceived() })

        flutterEngine.guard {
            throw RuntimeException("NSS engine failed to initialize!")
        }

        // Register channels.
        mViewModel.registerMethodChannels(flutterEngine!!)

        GigyaLogger.debug(LOG_TAG, "Registered nss method channels.")
        // Execute the engine's "main" method. Making sure that the main platform channel is already initialized
        // in the native side. Avoid signal -6 crash.
        flutterEngine!!.dartExecutor.executeDartEntrypoint(DartExecutor.DartEntrypoint(
                FlutterMain.findAppBundlePath(),
                "main")
        )
    }

    /**
//     * Engine notified that the main flow/work has ended.
     * Dismiss/destroy the activity.
     */
    private fun onFinishReceived() {
        finish()
    }

    //region Extensions

    /**
     * Wrapper inner class for attaching activity to a cached Flutter engine.
     */
    internal class NSSCachedEngineIntentBuilder :
            FlutterActivity.CachedEngineIntentBuilder(
                    NssActivity::class.java,
                    GigyaNss.FLUTTER_ENGINE_ID
            )

    /**
     * Wrapper inner class for initializing a new Flutter engine.
     */
    internal class NSSEngineIntentBuilder :
            FlutterActivity.NewEngineIntentBuilder(
                    NssActivity::class.java
            )

    //endregion
}