package com.gigya.android.sdk.tfa;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.containers.IoCContainer;

public class GigyaTFA {

    public static final String VERSION = "1.0.0";

    private static final String LOG_TAG = "GigyaBiometric";

    private static GigyaTFA _sharedInstance;

    public static synchronized GigyaTFA getInstance() {
        if (_sharedInstance == null) {
            IoCContainer container = Gigya.getContainer();

            container.bind(GigyaTFA.class, GigyaTFA.class, true);

            // Set the relevant biometric implementation according to Android API level.

            try {
                _sharedInstance = container.get(GigyaTFA.class);
            } catch (Exception e) {
                GigyaLogger.error(LOG_TAG, "Error creating Gigya Biometric SDK (did you forget to Gigya.setApplication?");
                e.printStackTrace();
                throw new RuntimeException("Error creating Gigya Biometric SDK (did you forget to Gigya.setApplication?");
            }
        }
        return _sharedInstance;
    }

    protected GigyaTFA() {

    }

    public void optInForPushTFA() {

    }

    public void optOutOfPushTFA() {

    }
}
