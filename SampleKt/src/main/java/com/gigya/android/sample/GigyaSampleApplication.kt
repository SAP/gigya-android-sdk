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
        // Initialization with implicit configuration & myAccountLiveData scheme.
        Gigya.getInstance(MyAccount::class.java)

        /*
        Initialization with implicit configuration & without a custom myAccountLiveData scheme.
        Will use the default GigyaAccount scheme.
        */
        // Gigya.getInstance(applicationContext)

        /*
        TFA phone/totp.
         */
        //Gigya.getInstance(MyAccount::class.java).init(getString(R.string.api_with_phone_totp_tfa), "us1-st1.gigya.com")

        /*
        TFA Email.
         */
        //Gigya.getInstance(MyAccount::class.java).init(getString(R.string.api_with_email_tfa));

        /*
        TFA push only (Staging).
         */
        //Gigya.getInstance(MyAccount::class.java).init("3_r0q_m0wQe7gyZJxHFL-mBAkPYILsmsIzloWwAB6QMXRiacHjq2f6CxBy0Mya9rLS", "ru1.gigya.com")

        /*
        With LiveLink.
         */
        //Gigya.getInstance(MyAccount::class.java).init("3_HCalFtInj--1FsLPEnTrEB0G1up6BDobQ16YVTt00VBL4DrFYK54Vy_g-zeu6gNw", "us1.gigya.com")

        /*
        Global login site
         */
        Gigya.getInstance(MyAccount::class.java).init("3_okzFVIQTsXw5vS6s0y9BEm6T4fbNTPVox6DZAwn-rCC7ca1dv6LhrPCdksCiSfOc", "us1-st2.gigya.com")
    }
}