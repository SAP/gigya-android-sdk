package com.gigya.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;

public class PersistenceHandler {

    /*
     * SDK shared preference file key for _session persistence.
     */
    private static final String PREFS_FILE_KEY = "GSLIB";

    private SharedPreferences _sharedPrefs;

    PersistenceHandler(Context context) {
        _sharedPrefs = context.getSharedPreferences(PREFS_FILE_KEY, Context.MODE_PRIVATE);
    }

    public String getString(String key, String fallback) {
        return _sharedPrefs.getString(key, fallback);
    }

    void onLoginProviderUpdated(String providerName) {
        _sharedPrefs.edit().putString("lastLoginProvider", providerName).apply();
    }

    /* Post logout actions. */
    void onLogout() {
        _sharedPrefs.edit().remove("lastLoginProvider").apply();
    }
}
