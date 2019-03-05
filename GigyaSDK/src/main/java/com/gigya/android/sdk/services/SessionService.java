package com.gigya.android.sdk.services;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.encryption.EncryptionException;
import com.gigya.android.sdk.encryption.IEncryptor;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.utils.CipherUtils;

import org.json.JSONObject;

import javax.crypto.SecretKey;

public class SessionService {

    private static final String LOG_TAG = "SessionService";

    /*
     * SDK shared preference key for _session string.
     */
    private static final String PREFS_KEY_SESSION = "GS_PREFS";

    /*
    Session info encryption algorithm.
     */
    private static final String ENCRYPTION_ALGORITHM = "AES";

    /*
    Main Gigya session reference.
     */
    @Nullable
    private SessionInfo _session;

    @Nullable
    public SessionInfo getSession() {
        return _session;
    }

    private IEncryptor _encryptor;

    @NonNull
    final private PersistenceService _persistenceService;

    @NonNull
    final private Config _config;

    @NonNull
    final private Context _appContext;

    public SessionService(@NonNull Context appContext, @NonNull Config config,
                          @NonNull PersistenceService persistenceService, IEncryptor encryptor) {
        _appContext = appContext;
        _config = config;
        _encryptor = encryptor;
        _persistenceService = persistenceService;
        // Get reference to SDK shared preference file.
        load();
    }

    /**
     * Get reference to current SDK configuration.
     * Only one configuration object is live during any SDK session.
     *
     * @return SDK Config reference.
     */
    public Config getConfig() {
        return _config;
    }


    /**
     * Set a new session reference.
     * Referenced object will be set & session info will persist.
     *
     * @param session New session reference.
     */
    public void setSession(@Nullable SessionInfo session) {
        GigyaLogger.debug(LOG_TAG, "setSession : " + session == null ? "null session" : session.toString());
        if (session != null) {
            _session = session;
            save();
        } else {
            GigyaLogger.error(LOG_TAG, "Failed to parse _session info from response");
        }
    }

    /**
     * Checks weather the current referenced session is valid or not.
     *
     * @return True if session is valid.
     */
    public boolean isValidSession() {
        return (_session != null && _session.isValid());
    }

    //region Session persistence


    /**
     * Clear current session.
     * Will nullify current reference & remove any records from encrypted persistence.
     */
    public void clear() {
        GigyaLogger.debug(LOG_TAG, "clear: ");
        _persistenceService.remove(PREFS_KEY_SESSION);
        this._session = null;
    }

    /**
     * Save session data to encrypted persistence store.
     */
    void save() {
        GigyaLogger.error(LOG_TAG, "save:");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sessionToken", _session != null ? _session.getSessionToken() : null);
            jsonObject.put("sessionSecret", _session != null ? _session.getSessionSecret() : null);
            jsonObject.put("expirationTime", _session != null ? _session.getExpirationTime() : null);
            jsonObject.put("ucid", _config.getUcid());
            jsonObject.put("gmid", _config.getGmid());
            /* Encrypt _session. */
            final String sessionJSON = jsonObject.toString();
            final String encryptedSession = encrypt(sessionJSON);
            /* Save to preferences. */
            _persistenceService.add(PREFS_KEY_SESSION, encryptedSession);
        } catch (Exception ex) {
            ex.printStackTrace();
            GigyaLogger.error(LOG_TAG, "sessionToJson: Error in conversion to " + ex.getMessage());
        }
    }

    /**
     * Check current persistent session is of Legacy type.
     * Legacy sessions are relevant only to SDK versions <=3.3.14.
     *
     * @return True if Application shared preferences contain a Legacy session entry.
     */
    private boolean isLegacySession() {
        return (!TextUtils.isEmpty(_persistenceService.getString("session.Token", null)));
    }

    /**
     * Load session reference.
     * Will check first if the current persist session is of Legacy type (SDK versions <= 3.3.14). If so will
     * load and migrate them to updated session structure & encryption.
     */
    private void load() {
        /* Check & load legacy session if available. */
        if (isLegacySession()) {
            GigyaLogger.debug(LOG_TAG, "load: isLegacySession!! Will migrate to update structure");
            loadLegacySession();
            return;
        }
        /* Load from preferences. */
        if (_persistenceService.contains(PREFS_KEY_SESSION)) {
            String encryptedSession = _persistenceService.getString(PREFS_KEY_SESSION, null);
            if (!TextUtils.isEmpty(encryptedSession)) {
                /* Decrypt _session string. */
                final String sessionJson = decrypt(encryptedSession);

                try {
                    JSONObject jsonObject = new JSONObject(sessionJson);
                    final String sessionToken = jsonObject.has("sessionToken") ? jsonObject.getString("sessionToken") : null;
                    final String sessionSecret = jsonObject.has("sessionSecret") ? jsonObject.getString("sessionSecret") : null;
                    final long expirationTime = jsonObject.has("expirationTime") ? jsonObject.getLong("expirationTime") : -1;
                    _session = new SessionInfo(sessionSecret, sessionToken, expirationTime);

                    final String ucid = jsonObject.getString("ucid");
                    _config.setUcid(ucid);
                    final String gmid = jsonObject.getString("gmid");
                    _config.setGmid(gmid);

                    GigyaLogger.debug(LOG_TAG, "Session load: " + _session.toString());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    GigyaLogger.error(LOG_TAG, "sessionToJson: Error in conversion from" + ex.getMessage());
                }
            }
        }
    }

    /**
     * Load legacy session entries and populate SessionInfo reference & ucid/gmid entries in the config reference.
     */
    private void loadLegacySession() {
        final String token = _persistenceService.getString("session.Token", null);
        final String secret = _persistenceService.getString("session.Secret", null);
        final long expiration = _persistenceService.getLong("session.ExpirationTime", 0L);
        _session = new SessionInfo(secret, token, expiration);
        final String ucid = _persistenceService.getString("ucid", null);
        _config.setUcid(ucid);
        final String gmid = _persistenceService.getString("gmid", null);
        _config.setGmid(gmid);
        /* Clear all legacy session entries. */
        _persistenceService.remove("ucid", "gmid", "lastLoginProvider", "session.Token",
                "session.Secret", "tsOffset", "session.ExpirationTime");
        /* Save session in current construct. */
        save();
    }

    //endregion

    //region Session encryption/decryption

    /**
     * Encrypt session secret using generated encryptor keys.
     *
     * @param plain Plain secret key.
     * @return Encrypted secret String.
     * @throws EncryptionException Multiple exception may throw.
     */
    private String encrypt(String plain) throws EncryptionException {
        GigyaLogger.debug(LOG_TAG, ENCRYPTION_ALGORITHM + " encrypt: ");
        try {
            final SecretKey secretKey = _encryptor.getKey(_appContext, _persistenceService);
            return CipherUtils.encrypt(plain, ENCRYPTION_ALGORITHM, secretKey);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("Session encryption exception", ex.getCause());
        }
    }

    /**
     * Decrypt session secret using generated encryptor keys.
     *
     * @param encrypted Encrypted value.
     * @return Decrypted String secret.
     * @throws EncryptionException Multiple exception may throw.
     */
    private String decrypt(String encrypted) throws EncryptionException {
        GigyaLogger.debug(LOG_TAG, ENCRYPTION_ALGORITHM + " decrypt: ");
        try {
            final SecretKey secretKey = _encryptor.getKey(_appContext, _persistenceService);
            return CipherUtils.decrypt(encrypted, ENCRYPTION_ALGORITHM, secretKey);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new EncryptionException("Session encryption exception", ex.getCause());
        }
    }

    //endregion
}
