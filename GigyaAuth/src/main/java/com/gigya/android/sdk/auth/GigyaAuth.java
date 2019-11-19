package com.gigya.android.sdk.auth;

import android.annotation.SuppressLint;
import android.content.Context;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.push.IGigyaNotificationManager;

public class GigyaAuth {

    private static final String VERSION = "1.0.0";

    private static final String LOG_TAG = "GigyaAuth";

    @SuppressLint("StaticFieldLeak")
    private static GigyaAuth _sharedInstance;

    public static synchronized GigyaAuth getInstance() {
        if (_sharedInstance == null) {
            IoCContainer container = Gigya.getContainer();

            container.bind(GigyaAuth.class, GigyaAuth.class, true);

            try {
                _sharedInstance = container.get(GigyaAuth.class);
                GigyaLogger.debug(LOG_TAG, "Instantiation version: " + VERSION);
            } catch (Exception e) {
                GigyaLogger.error(LOG_TAG, "Error creating Gigya Auth library (did you forget to Gigya.setApplication?");
                e.printStackTrace();
                throw new RuntimeException("Error creating Gigya Auth library (did you forget to Gigya.setApplication?");
            }
        }
        return _sharedInstance;
    }

    private final Context _context;
    private final IGigyaNotificationManager _gigyaNotificationManager;

    protected GigyaAuth(Context context,
                        IGigyaNotificationManager gigyaNotificationManager) {
        _context = context;
        _gigyaNotificationManager = gigyaNotificationManager;

    }

}
