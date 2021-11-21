package com.gigya.android.sample

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.oidc.OIDCViewModel
import com.gigya.android.sdk.utils.UrlUtils
import java.util.HashMap

class GigyaOIDCActivity : AppCompatActivity() {

    var builder = CustomTabsIntent.Builder()

    lateinit var oidcViewModel: OIDCViewModel

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val uri: Uri = intent?.data as Uri
        val parsed: Map<String, Any> = HashMap()
        UrlUtils.parseUrlParameters(parsed, uri.query)

        if (parsed.containsKey("code")) {
            oidcViewModel.authenticateWith(parsed["code"] as String) {
                this.finish()
            }
        } else {
            //TODO ERROR
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        oidcViewModel = Gigya.getContainer().get(OIDCViewModel::class.java)

        val frame = FrameLayout(this)
        this.setContentView(frame)

        val authUrl = oidcViewModel.getAuthorizeUrl()
        val customTabsIntent: CustomTabsIntent = builder.build()
        customTabsIntent.launchUrl(this, Uri.parse(authUrl))
    }
}