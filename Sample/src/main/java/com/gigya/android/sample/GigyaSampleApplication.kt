package com.gigya.android.sample

import android.app.Application
import android.util.Log
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.log.GigyaLogger

class GigyaSampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        GigyaLogger.setDebugMode(true)
        Log.d("GigyaSampleApplication", Gigya.VERSION)

        /*
        Initialization with explicit api-key.
         */
        //Gigya.getInstance(applicationContext).init(getString(R.string.api_kay))

        /*
        Initialization with explicit api-key, api-domain type.
        */
        //Gigya.getInstance(applicationContext).init(getString(R.string.api_kay), getString(R.string.api_domain)))

        /*
        Initialization with implicit configuration.
        */
        Gigya.getInstance(applicationContext)

        /*
        Initialization with implicit configuration & account scheme.
         */
        //Gigya.getInstance(applicationContext, MyAccount::class.java)
    }
}