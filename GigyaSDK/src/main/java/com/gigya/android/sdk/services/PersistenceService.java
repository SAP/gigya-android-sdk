package com.gigya.android.sdk.services;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Service for accessing Gigya context specific shared preference persistence.
 */
public class PersistenceService {

    /*
     * SDK shared preference file key for _session persistence.
     */
    private static final String PREFS_FILE_KEY = "GSLIB";

    /*
     * SDK shared preference key for _session string.
     */
    private static final String PREFS_KEY_SESSION = "GS_PREFS";

    @NonNull
    private SharedPreferences _prefs;

    public PersistenceService(Context appContext) {
        _prefs = appContext.getSharedPreferences(PREFS_FILE_KEY, Context.MODE_PRIVATE);
    }

    //region Object access.

    /**
     * Check element availability by given key.
     *
     * @param key Specified key.
     * @return True if available.
     */
    public boolean contains(String key) {
        return _prefs.contains(key);
    }

    /**
     * Request a String object from persistence.
     *
     * @param key      Specified key.
     * @param fallback Fallback String value.
     * @return String requested or fallback value if not available.
     */
    @Nullable
    public String getString(String key, @Nullable String fallback) {
        return _prefs.getString(key, fallback);
    }

    /**
     * Request a Long object from persistence.
     *
     * @param key      Specified key.
     * @param fallback Fallback long value.
     * @return long requested or fallback value if not available.
     */
    public long getLong(String key, @NonNull Long fallback) {
        return _prefs.getLong(key, fallback);
    }

    /**
     * Add String element to persistence given key value.
     *
     * @param key     Specified key.
     * @param element String element to persist.
     */
    public void add(String key, String element) {
        _prefs.edit().putString(key, element).apply();
    }

    /**
     * Remove multiple String elements from persistence.
     *
     * @param keys Specified keys (varargs).
     */
    public void remove(String... keys) {
        final SharedPreferences.Editor editor = _prefs.edit();
        for (String key : keys) {
            editor.remove(key);
        }
        editor.apply();
    }

    //endregion

    // Session specific.

    public void setSession(String encrypted) {
        add(PREFS_KEY_SESSION, encrypted);
    }

    @Nullable
    public String getSession() {
        return getString(PREFS_KEY_SESSION, null);
    }

    public void clearSession() {
        remove(PREFS_KEY_SESSION);
    }

    public boolean hasSession() {
        return contains(PREFS_KEY_SESSION);
    }

    private static final String LAST_LOGIN_PROVIDER_KEY = "lastLoginProvider";

    /**
     * On account logout, Remove last login provider reference.
     */
    public void onAccountLogout() {
        remove(LAST_LOGIN_PROVIDER_KEY);
    }

    /**
     * Add last login provider reference when available and verified.
     *
     * @param socialProvider Social login provider name.
     */
    public void updateLastSocialLoginProvider(String socialProvider) {
        add(LAST_LOGIN_PROVIDER_KEY, socialProvider);
    }

    //endregion
}
