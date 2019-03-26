package com.gigya.android.sample

import android.annotation.SuppressLint
import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import android.webkit.WebView
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger

@Suppress("unused") // Referenced in manifest.
class GigyaSampleApplication : Application() {

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate() {
        super.onCreate()

        // Allow WebViews debugging.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (0 != applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
        }

        GigyaLogger.setDebugMode(true)
        Log.d("GigyaSampleApplication", Gigya.VERSION)

        /*
        Initialization with implicit configuration & account scheme.
         */
        Gigya.getInstance(applicationContext, MyAccount::class.java)

        /*
        Initialization with implicit configuration & without a custom account scheme.
        Will use the default GigyaAccount scheme.
        */
       // Gigya.getInstance(applicationContext)

        /*
        Explicit initialization.
         */
        //Gigya.getInstance(applicationContext, MyAccount::class.java).init(getString(R.string.api_with_phone_totp_tfa))
        //Gigya.getInstance(applicationContext, MyAccount::class.java).init(getString(R.string.api_key_eu), "eu1-st1.gigya.com")
    }
}