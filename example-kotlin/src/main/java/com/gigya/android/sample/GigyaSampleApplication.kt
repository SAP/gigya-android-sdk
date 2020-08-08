package com.gigya.android.sample

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.util.Log
import android.webkit.WebView
import com.gigya.android.sample.model.MyAccount
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.persistence.IPersistenceService
import com.gigya.android.sdk.session.SessionService

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
        Gigya.secureSdkActivities(true)

        //val sessionExpiration = Gigya.getContainer().get(IPersistenceService::class.java).sessionExpiration

        // Initialization with implicit configuration & myAccountLiveData scheme.
        Gigya.getInstance(MyAccount::class.java)

        //if (gigya.isLoggedIn &&  sessionExpiration != 0L && sessionExpiration < System.currentTimeMillis()) {
        //    Gigya.getInstance(MyAccount::class.java).logout()
        //}

        val signatureHelper = AppSignatureHelper(this)
        GigyaLogger.debug("GigyaSampleApplication SIG", signatureHelper.getAppSignatures().toString())

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
        //Gigya.getInstance(MyAccount::class.java).init(getString(R.string.apui_with_email_tfa));

        /*
        TFA push only (Staging).
         */
        //Gigya.getInstance(MyAccount::class.java).init("3_r0q_m0wQe7gyZJxHFL-mBAkPYILsmsIzloWwAB6QMXRiacHjq2f6CxBy0Mya9rLS", "ru1.gigya.com")

        /*
        With LiveLink.
         */
        //Gigya.getInstance(MyAccount::class.java).init("3_7iTV4NJUApz8TdUB5_Ms-SH2Fuj3aUgfw-y78IYulD-BA8IFLYRcpUDoCwzDgs9o", "us1.gigya.com")

        /*
        Global login site
         */
        //Gigya.getInstance(MyAccount::class.java).init("4_mOdtipUxEhwsuZ6McabFRQ")

        /*
        Forcing pending registration
         */
        //Gigya.getInstance(MyAccount::class.java).init("3_5FIA8jNk1LTw_spOzIYLVU-GJVm93TAfJjqTjACQCnfxfRm0kZghGH1lx7zoNMbD")

        /*
        SAML
         */
        //Gigya.getInstance(MyAccount::class.java).init("3_e6fBzFpWA_5w1A2aB96Ad3NGmlbg6r1iQ5JQHv78tGj_YaYr1laPmM5XA3hO2YNh")

        /*
        Eu site testing.
         */
        //Gigya.getInstance(MyAccount::class.java).init("3_1pjil0H2GoDmHPAAMsqY24BdoNNAqBEFO4n461JnkbarkP2DBKoOpFzaxekXt3kt", "eu1.gigya.com")

        /*
        Auth
         */
        //Gigya.getInstance(MyAccount::class.java).init("3_fkRaJs5vQuVjkb9Z1He22lHMUcJAwfZMUDvIRQ08Jhnk82yAWR5ZapM44N332hy9", "us1-st1.gigya.com")
    }
}