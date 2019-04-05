package com.gigya.android.sdk.persistence;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class PersistenceService implements IPersistenceService {

    private SharedPreferences _prefs;

    final private Context _context;

    public PersistenceService(Context context) {
        _context = context;
    }

    private SharedPreferences getPrefs() {
        if (_prefs == null) {
            _prefs = _context.getSharedPreferences(PREFS_FILE_KEY, Context.MODE_PRIVATE);
        }
        return _prefs;
    }


    @Override
    public boolean contains(String key) {
        return _prefs.contains(key);
    }

    @Override
    public String getString(String key, String defValue) {
        return getPrefs().getString(key, defValue);
    }

    @Override
    public Long getLong(String key, Long defValue) {
        return getPrefs().getLong(key, defValue);
    }

    @Override
    public void add(String key, Object element) {
        final SharedPreferences.Editor editor = getPrefs().edit();
        if (element instanceof String) {
            editor.putString(key, (String) element);
        } else if (element instanceof Long) {
            editor.putLong(key, (Long) element);
        }
        editor.apply();
    }

    @Override
    public void remove(String... keys) {
        final SharedPreferences.Editor editor = getPrefs().edit();
        for (String key : keys) {
            editor.remove(key);
        }
        editor.apply();
    }

    @Override
    public Set<String> getSet(String key, Set<String> defValue) {
        return getPrefs().getStringSet(key, defValue);
    }

    @Override
    public Set<String> getSocialProviders() {
        return _prefs.getStringSet(PREFS_KEY_PROVIDER_SET, null);
    }

    @Override
    public void addSocialProvider(String provider) {
        Set<String> providerSet = getSocialProviders();
        if (providerSet == null) {
            providerSet = new HashSet<>();
        }
        providerSet.add(provider);
        _prefs.edit().putStringSet(PREFS_KEY_PROVIDER_SET, providerSet).apply();
    }

    //region KEYS

    /*
     * File key for SDK preferences persistence.
     */
    public static final String PREFS_FILE_KEY = "GSLIB";

    /*
     * Value key for Session persistence.
     */
    public static final String PREFS_KEY_SESSION = "GS_PREFS";

    /*
     * Value key for last used social providers.
     */
    public static final String PREFS_KEY_PROVIDER_SET = "GS_PROVIDER_SET";

    /*
     * Value key for session expiration timestamp.
     */
    public static final String PREFS_KEY_SESSION_EXPIRE_TIMESTAMP = "GS_SESSION_EXPIRE_TIMESTAMP";

    /*
   Value key for session encryption type. Value is taken from legacy version 3 to allow upgrading from older SDK versions.
    */
    public static final String PREFS_KEY_SESSION_ENCRYPTION_TYPE = "sessionProtectionType";

    /*
     * Value key for biometric cipher iv spec.
     */
    public static final String PREFS_KEY_IV_SPEC = "IV_fingerprint";

    //endregion
}
