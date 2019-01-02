package com.gigya.android.sdk.log;

import android.util.Log;

public class GigyaLogger {

    private static final String LOG_TAG = "GigyaSDK";
    private static boolean DEBUG = false;

    public static void setDebugMode(boolean debugModeEnabled) {
        DEBUG = debugModeEnabled;
    }

    public static boolean isDebug() {
        return DEBUG;
    }

    public static void debug(String tag, String message) {
        if (DEBUG) {
            Log.d(LOG_TAG, logMessage(tag, message));
        }
    }

    public static void error(String tag, String message) {
        if (DEBUG) {
            Log.e(LOG_TAG, logMessage(tag, message));
        }
    }

    private static String logMessage(String classTAG, String message) {
        return "<<< " + classTAG + " *** " + message + " >>>";
    }
}
