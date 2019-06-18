package com.gigya.android.sample;

import android.app.Application;
import android.util.Log;
import android.webkit.WebView;

import com.gigya.android.sample.model.MyAccount;
import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;

public class GigyaSampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Allow WebViews debugging.
        WebView.setWebContentsDebuggingEnabled(true);
        // Gigya logs.
        GigyaLogger.setDebugMode(true);
        Log.d("GigyaSampleApplication", Gigya.VERSION);

        Gigya.setApplication(this);
        // Initialization with implicit configuration & account scheme.
        Gigya.getInstance(MyAccount.class);


    }
}
