package com.gigya.android.sample.ui

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.gigya.android.sample.R
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaPluginCallback
import com.gigya.android.sdk.ui.plugin.GigyaWebBridge
import kotlinx.android.synthetic.main.activity_web_bridge.*
import org.jetbrains.anko.design.snackbar

class WebBridgeTestActivity : AppCompatActivity() {

    private var _webBridge: GigyaWebBridge<MyAccount>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_bridge)

        initUi()
    }

    private fun initUi() {

        val webSettings = web_view.settings
        webSettings.javaScriptEnabled = true

        // Generate a new web bridge instance.
        _webBridge = Gigya.getInstance(MyAccount::class.java).createWebBridge()
        _webBridge?.attachTo(web_view, false, object : GigyaPluginCallback<MyAccount>() {


            override fun onLogin(accountObj: MyAccount) {
                web_view.snackbar("onLogin event shown")
            }


        }, null, null)


        web_view.webViewClient = (object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.getUrl()
                val uriString = uri.toString()
                _webBridge?.invoke(uriString)
                return true
            }

            override fun shouldOverrideUrlLoading(view: WebView?, urlString: String?): Boolean {
                val uri = Uri.parse(urlString)
                _webBridge?.invoke(uri.toString())
                return true
            }
        })

//        web_view.loadUrl("https://gigyademo.com")
        web_view.loadUrl("http://10.27.65.167:3333")
    }


}