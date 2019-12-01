package com.gigya.android.sdk.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaDefinitions;

import java.util.HashSet;
import java.util.Set;

public class PersistenceService implements IPersistenceService {

    private SharedPreferences _prefs;

    final private Context _context;

    public PersistenceService(Context context) {
        _context = context;
    }

    protected SharedPreferences getPrefs() {
        if (_prefs == null) {
            _prefs = _context.getSharedPreferences(PREFS_FILE_KEY, Context.MODE_PRIVATE);
        }
        return _prefs;
    }

    //region HELPERS

    /**
     * Check if session persistence is available.
     */
    @Override
    public boolean isSessionAvailable() {
        return contains(PREFS_KEY_SESSION);
    }

    /**
     * Persist a new encrypted session.
     *
     * @param encryptedSession Encrypted session String.
     */
    @Override
    public void setSession(String encryptedSession) {
        add(PREFS_KEY_SESSION, encryptedSession);
    }

    /**
     * Get persistent session.
     *
     * @return Encrypted session String or null if session persistence exists.
     */
    @Override
    public String getSession() {
        return getString(PREFS_KEY_SESSION, null);
    }

    /**
     * Set session expiration timestamp.
     *
     * @param expiration Expiration timestamp (Long).
     */
    @Override
    public void setSessionExpiration(long expiration) {
        add(PREFS_KEY_SESSION_EXPIRE_TIMESTAMP, expiration);
    }

    /**
     * Get session expiration timestamp.
     *
     * @return Persistent session expiration or 0 if timestamp does not exist.
     */
    @Override
    public long getSessionExpiration() {
        return getLong(PREFS_KEY_SESSION_EXPIRE_TIMESTAMP, 0L);
    }

    /**
     * Remove session from persistence store.
     */
    @Override
    public void removeSession() {
        remove(PREFS_KEY_SESSION);
    }

    /**
     * Remove legacy session data if from persistence store.
     */
    @Override
    public void removeLegacySession() {
        remove("ucid", "gmid", "lastLoginProvider", "session.Token",
                "session.Secret", "tsOffset", "session.ExpirationTime");
    }

    /**
     * Update session encryption type.
     *
     * @param encryptionType Encryption type String identifier.
     */
    @Override
    public void setSessionEncryptionType(String encryptionType) {
        add(PREFS_KEY_SESSION_ENCRYPTION_TYPE, encryptionType);
    }

    /**
     * Get session encryption type.
     *
     * @return Encryption type String identifier or "DEFAULT" if does not exist.
     */
    @Override
    public String getSessionEncryptionType() {
        return getString(PREFS_KEY_SESSION_ENCRYPTION_TYPE, GigyaDefinitions.SessionEncryption.DEFAULT);
    }

    /**
     * Get social providers identifiers what were used.
     *
     * @return Set of provider identifiers or null if none exist.
     */
    @Override
    public Set<String> getSocialProviders() {
        return getPrefs().getStringSet(PREFS_KEY_PROVIDER_SET, new HashSet<String>());
    }

    /**
     * Add a used social provider identifier.
     *
     * @param provider Provider identifier name {@link com.gigya.android.sdk.GigyaDefinitions.Providers}
     */
    @Override
    public void addSocialProvider(String provider) {
        Set<String> providerSet = getSocialProviders();
        if (providerSet == null) {
            providerSet = new HashSet<>();
        }
        providerSet.add(provider);
        getPrefs().edit().putStringSet(PREFS_KEY_PROVIDER_SET, providerSet).apply();
    }

    /**
     * Remove all saved social provider identifiers.
     * Will be called after logout.
     */
    @Override
    public void removeSocialProviders() {
        remove(PREFS_KEY_PROVIDER_SET);
    }

    //endregion

    //region PRIVATE HELPERS

    private boolean contains(String key) {
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

    private void remove(String... keys) {
        final SharedPreferences.Editor editor = getPrefs().edit();
        for (String key : keys) {
            editor.remove(key);
        }
        editor.apply();
    }

    private Set<String> getSet(String key, Set<String> defValue) {
        return getPrefs().getStringSet(key, defValue);
    }

    @Override
    public void setPushToken(String pushToken) {
        getPrefs().edit().putString(PREFS_PUSH_TOKEN, pushToken).apply();
    }

    @Nullable
    @Override
    public String getPushToken() {
        return getPrefs().getString(PREFS_PUSH_TOKEN, null);
    }

    //endregion

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

    /*
     * Push token key.
     */
    private static final String PREFS_PUSH_TOKEN = "GS_PUSH_TOKEN";

    //endregion
}
