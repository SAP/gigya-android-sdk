package com.gigya.android.sample

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import android.webkit.WebView
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger

/**
 * Application extender class.
 * Initialize all SDKs in this class to avoid concurrent context dependant initializations.
 */
@Suppress("unused") // Referenced in manifest.
class GigyaSampleApplication : Application() {

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate() {
        super.onCreate()

        // Allow WebViews debugging.
        WebView.setWebContentsDebuggingEnabled(true)

        // Gigya logs.
        GigyaLogger.setDebugMode(true)
        GigyaLogger.enableSmartLog(this)
        Log.d("GigyaSampleApplication", Gigya.VERSION)

        Gigya.setApplication(this)
        Gigya.secureActivityWindow(true)

        // Initialization with implicit configuration & myAccountLiveData scheme.
        Gigya.getInstance(MyAccount::class.java)

        val signatureHelper = AppSignatureHelper(this)
        GigyaLogger.debug("GigyaSampleApplication SIG", signatureHelper.getAppSignatures().toString())
    }
}