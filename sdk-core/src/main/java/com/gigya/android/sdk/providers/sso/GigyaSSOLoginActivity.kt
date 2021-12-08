package com.gigya.android.sdk.providers.sso

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.ui.Presenter
import com.gigya.android.sdk.ui.WebLoginActivity
import com.gigya.android.sdk.utils.UrlUtils

class GigyaSSOLoginActivity : AppCompatActivity() {

    companion object {
        private const val LOG_TAG = "GigyaSSOLoginActivity"
        private const val EXTRA_LIFECYCLE_CALLBACK_ID = "sso_login_lifecycle_callback"
        private const val EXTRA_URI = "sso_login_uri"

        fun present(context: Context, uri: String?, lifecycleCallback: SSOLoginActivityCallback) {
            val intent = Intent(context, GigyaSSOLoginActivity::class.java)
            intent.putExtra(EXTRA_LIFECYCLE_CALLBACK_ID, Presenter.addSSOLoginLifecycleCallback(lifecycleCallback))
            intent.putExtra(EXTRA_URI, uri)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
            context.startActivity(intent)
        }

    }

    interface SSOLoginActivityCallback {
        fun onResult(activity: Activity?, parsed: Map<String, Any>)
        fun onCancelled()
    }

    var builder = CustomTabsIntent.Builder()

    private var _ssoLoginLifecycleCallbacks: SSOLoginActivityCallback? = null
    private var _ssoLoginLifecycleCallbacksId = -1
    private var _uri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent == null) {
            GigyaLogger.debug(LOG_TAG, "Intent null")
            finish()
            return
        }
        if (intent.extras == null) {
            GigyaLogger.debug(LOG_TAG, "Intent extras null")
            finish()
            return
        }

        _ssoLoginLifecycleCallbacksId = intent.getIntExtra(EXTRA_LIFECYCLE_CALLBACK_ID, -1)
        if (_ssoLoginLifecycleCallbacksId == -1) {
            GigyaLogger.debug(LOG_TAG, "web_login_lifecycle_callback null")
            finish()
            return
        }

        _uri = intent.getStringExtra(EXTRA_URI)
        if (_uri == null) {
            GigyaLogger.debug(LOG_TAG, "web_login_uri null")
            finish()
            return
        }

        // Reference the callback using static getter from the Presenter. Same as the HostActivity.

        // Reference the callback using static getter from the Presenter. Same as the HostActivity.
        _ssoLoginLifecycleCallbacks = Presenter.getSSOLoginCallback(_ssoLoginLifecycleCallbacksId)

        // Authenticate using custom tabs instance.
        // Custom tabs instance is currently not customizable.
        val customTabsIntent: CustomTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(_uri))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Parse Uri query parameters.
        val uri: Uri = intent?.data as Uri
        val queryParams: Map<String, Any> = mutableMapOf()
        UrlUtils.parseUrlParameters(queryParams, uri.query)

        _ssoLoginLifecycleCallbacks?.onResult(this, queryParams)
        finish()
    }

    override fun onBackPressed() {
        // Throttle cancel event.
        if (!isFinishing) {
            _ssoLoginLifecycleCallbacks?.onCancelled()
        }
        super.onBackPressed()
    }

    override fun finish() {
        Presenter.flushSSOLoginLifecycleCallback(_ssoLoginLifecycleCallbacksId)
        super.finish()
        /*
        Disable exit animation.
         */overridePendingTransition(0, 0)
    }
}