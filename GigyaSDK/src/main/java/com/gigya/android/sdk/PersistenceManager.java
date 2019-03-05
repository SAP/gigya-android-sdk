package com.gigya.android.sdk;

import android.content.Context;
import android.content.SharedPreferences;

@Deprecated
public class PersistenceManager {

    /*
     * SDK shared preference file key for _session persistence.
     */
    private static final String PREFS_FILE_KEY = "GSLIB";

    private SharedPreferences _sharedPrefs;

    PersistenceManager(Context context) {
        _sharedPrefs = context.getSharedPreferences(PREFS_FILE_KEY, Context.MODE_PRIVATE);
    }

    /* Post logout actions. */
    void onLogout() {
        _sharedPrefs.edit().remove("lastLoginProvider").apply();
    }

    //region Utility methods

    public String getString(String key, String fallback) {
        return _sharedPrefs.getString(key, fallback);
    }

    public long getLong(String key, Long fallback) {
        return _sharedPrefs.getLong(key, fallback);
    }

    public boolean contains(String key) {
        return _sharedPrefs.contains(key);
    }

    public void remove(String... keys) {
        final SharedPreferences.Editor editor = _sharedPrefs.edit();
        for (String key : keys) {
            editor.remove(key);
        }
        editor.apply();
    }

    public void add(String key, String element) {
        _sharedPrefs.edit().putString(key, element).apply();
    }

    //endregion

    //region Login providers

    public void onLoginProviderUpdated(String providerName) {
        _sharedPrefs.edit().putString("lastLoginProvider", providerName).apply();
    }

    //endregion
}
