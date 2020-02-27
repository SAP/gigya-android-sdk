package com.gigya.android.sdk.nss

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refine
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.FlutterEngineCache
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.view.FlutterMain

class NssActivity<T : GigyaAccount> : FlutterActivity() {

    private var mViewModel: NssViewModel<T>? = null

    companion object {

        const val LOG_TAG = "NativeScreenSetsActivity"

        // Extras.
        private const val EXTRA_INITIAL_ROUTE = "extra_initial_route"
        private const val EXTRA_MARKUP = "extra_markup"

        fun start(context: Context, markup: String, initialRoute: String?) {
            val intent: Intent = NSSCachedEngineIntentBuilder().build(context)
            initialRoute?.let {
                intent.putExtra(EXTRA_INITIAL_ROUTE, it)
            }
            intent.putExtra(EXTRA_MARKUP, markup)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val markup = intent?.extras?.getString(EXTRA_MARKUP)
        markup.guard {
            throw RuntimeException("Missing markup. Please provide markup on activity instantiation")
        }

        flutterEngine.guard {
            throw RuntimeException("NSS engine failed to initialize!")
        }

        GigyaNss.dependenciesContainer.get(NssViewModel::class.java).refine<NssViewModel<T>> {
            mViewModel = this
            mViewModel!!.markup = markup
            mViewModel!!.finish = {
                onFinishReceived()
            }

            // Add optional initial route.
            val initial = intent?.extras?.getString(EXTRA_INITIAL_ROUTE)
            initial?.let { route ->
                mViewModel!!.initialRoute = route
            }
        }

        // Load channels.
        mViewModel?.loadChannels(flutterEngine!!)

        GigyaLogger.debug(LOG_TAG, "Registered nss method channels.")
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
    }

    /**
     * Engine notified that the main flow/work has ended.
     * Dismiss/destroy the activity.
     */
    private fun onFinishReceived() {
        finish()
    }

    override fun onDestroy() {
        mViewModel?.dispose()
        flutterEngine?.destroy()
        FlutterEngineCache
                .getInstance().remove(GigyaNss.FLUTTER_ENGINE_ID)
        super.onDestroy()
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