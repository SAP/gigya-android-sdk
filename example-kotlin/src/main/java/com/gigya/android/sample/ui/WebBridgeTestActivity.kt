package com.gigya.android.sample.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import com.gigya.android.sample.R
import com.gigya.android.sample.extras.gone
import com.gigya.android.sample.extras.visible
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaPluginCallback
import com.gigya.android.sdk.ui.plugin.IGigyaWebBridge

class WebBridgeTestActivity : AppCompatActivity() {

    companion object {

        const val TEST_BRIDGE_URL = "CUSTOM_URL"
    }

    private var _webBridge: IGigyaWebBridge<MyAccount>? = null

    private var webView: WebView? = null
    private var progressIndicator: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_bridge)

        initUi()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initUi() {

        webView = findViewById(R.id.web_view)
        val webSettings = webView!!.settings
        webSettings.javaScriptEnabled = true

        // Generate a new web bridge instance.
        _webBridge = Gigya.getInstance(MyAccount::class.java).createWebBridge()

        // Attach web view to the web bridge instance.
        _webBridge?.attachTo(
                webView!!,
                object : GigyaPluginCallback<MyAccount>() {

                    /*
                    All GigyaPluginCallback methods are optional & available to override.
                     */

                    override fun onLogin(accountObj: MyAccount) {
                        toast("onLogin event shown")
                        onBackPressed()
                    }

                    override fun onLogout() {
                        toast("onLogout event shown")
                        onBackPressed()
                    }

                },
                progressIndicator!!)


        // Setup a web view client to allow web bridge URL exchange.
        webView!!.webViewClient = (object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
               progressIndicator!!.visible()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressIndicator!!.gone()
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

        webView!!.loadUrl(TEST_BRIDGE_URL)
    }

    override fun onDestroy() {
        _webBridge?.detachFrom(webView!!)
        super.onDestroy()
    }

    private fun toast(text: String) {
        val toast = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
        toast.show()
    }
}