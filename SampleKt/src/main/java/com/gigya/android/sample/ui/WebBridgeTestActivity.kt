package com.gigya.android.sample.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.gone
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaPluginCallback
import com.gigya.android.sdk.ui.plugin.GigyaWebBridge
import com.gigya.android.sdk.ui.plugin.IGigyaWebBridge
import kotlinx.android.synthetic.main.activity_web_bridge.*
import org.jetbrains.anko.design.snackbar

class WebBridgeTestActivity : AppCompatActivity() {

    companion object {

        const val TEST_BRIDGE_URL = "CUSTOM_URL"
    }

    private var _webBridge: IGigyaWebBridge<MyAccount>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_bridge)

        initUi()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initUi() {

        val webSettings = web_view.settings
        webSettings.javaScriptEnabled = true

        // Generate a new web bridge instance.
        _webBridge = Gigya.getInstance(MyAccount::class.java).createWebBridge()

        // Attach web view to the web bridge instance.
        _webBridge?.attachTo(
                web_view,
                object : GigyaPluginCallback<MyAccount>() {

                    /*
                    All GigyaPluginCallback methods are optional & available to override.
                     */

                    override fun onLogin(accountObj: MyAccount) {
                        web_view.snackbar("onLogin event shown")
                    }

                    override fun onLogout() {
                        web_view.snackbar("onLogout event shown")
                    }


                },
                progress_indicator,
                null)


        // Setup a web view client to allow web bridge URL exchange.
        web_view.webViewClient = (object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progress_indicator.visible()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progress_indicator.gone()
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url
                val uriString = uri.toString()
                return _webBridge?.invoke(uriString) ?: false
            }

            override fun shouldOverrideUrlLoading(view: WebView?, urlString: String?): Boolean {
                val uri = Uri.parse(urlString)
                return _webBridge?.invoke(uri.toString()) ?: false
            }
        })

        /* Load custom URL */

        web_view.loadUrl(TEST_BRIDGE_URL)
    }

    override fun onDestroy() {
        _webBridge?.detachFrom(web_view)
        super.onDestroy()
    }

}