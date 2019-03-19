package com.gigya.android.sdk.services;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.encryption.EncryptionException;
import com.gigya.android.sdk.encryption.IEncryptor;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.utils.CipherUtils;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import javax.crypto.SecretKey;

public class SessionService {

    private static final String LOG_TAG = "SessionService";

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

    @NonNull
    final private Runnable _newSessionRunnable;

    public SessionService(@NonNull Context appContext, @NonNull Config config,
                          @NonNull PersistenceService persistenceService, IEncryptor encryptor, @NonNull Runnable newSessionRunnable) {
        _appContext = appContext;
        _config = config;
        _encryptor = encryptor;
        _persistenceService = persistenceService;
        _newSessionRunnable = newSessionRunnable;
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
     * Get reference to persistence service.
     */
    @NonNull
    public PersistenceService getPersistenceService() {
        return _persistenceService;
    }

    /**
     * Set a new session reference.
     * Referenced object will be set & session info will persist.
     * Make sure to call this method only when a brand new session data is available and not an existing reference!!
     *
     * @param session New session reference.
     */
    public void setSession(@Nullable SessionInfo session) {
        if (session == null) {
            GigyaLogger.error(LOG_TAG, "Failed to parse _session info from response");
            return;
        }
        _session = session;
        if (_session.getExpirationTime() > 0) {
            _sessionWillExpireIn = System.currentTimeMillis() + (_session.getExpirationTime() * 1000);
            startSessionCountdownTimerIfNeeded();
        }
        save();
        GigyaLogger.debug(LOG_TAG, session.toString());

        // Run new session task. Will trigger session verification interval if needed.
        _newSessionRunnable.run();
    }

    /**
     * Checks weather the current referenced session is valid or not.
     *
     * @return True if session is valid.
     */
    public boolean isValidSession() {
        boolean valid = _session != null && _session.isValid();
        if (_sessionWillExpireIn > 0) {
            valid = System.currentTimeMillis() < _sessionWillExpireIn;
        }
        return valid;
    }

    //region SESSION PERSISTENCE


    /**
     * Clear current session.
     * Will nullify current reference & remove any records from encrypted persistence.
     */
    public void clear() {
        GigyaLogger.debug(LOG_TAG, "clear: ");
        _persistenceService.clearSession();
        this._session = null;
    }

    /**
     * Save session data to encrypted persistence store.
     */
    public void save() {
        GigyaLogger.debug(LOG_TAG, "save:");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sessionToken", _session != null ? _session.getSessionToken() : null);
            jsonObject.put("sessionSecret", _session != null ? _session.getSessionSecret() : null);
            jsonObject.put("expirationTime", _session != null ? _session.getExpirationTime() : null);
            jsonObject.put("ucid", _config.getUcid());
            jsonObject.put("gmid", _config.getGmid());

            // Encrypt _session.
            final String sessionJSON = jsonObject.toString();
            final String encryptedSession = encrypt(sessionJSON);

            // Save to preferences.
            _persistenceService.setSession(encryptedSession);

            // Persist session expiration if available.
            if (_sessionWillExpireIn > 0) {
                _persistenceService.setSessionExpiration(_sessionWillExpireIn);
            }
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
        if (_persistenceService.hasSession()) {
            String encryptedSession = _persistenceService.getSession();
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

                    // Get session expiration if exists.
                    _sessionWillExpireIn = _persistenceService.getSessionExpiration();
                    // Check if already passed. Rest if so.
                    if (_sessionWillExpireIn > 0 && _sessionWillExpireIn < System.currentTimeMillis()) {
                        _persistenceService.setSessionExpiration(_sessionWillExpireIn = 0);
                    }

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

    //region SESSION ENCRYPTION/DECRYPTION

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

    //region SESSION EXPIRATION TRACKING

    private long _sessionWillExpireIn = 0;

    private CountDownTimer _sessionLifeCountdownTimer;

    /**
     * Cancel running timer if reference is not null.
     */
    public void cancelSessionCountdownTimer() {
        if (_sessionLifeCountdownTimer != null) _sessionLifeCountdownTimer.cancel();
    }

    /**
     * Check if session countdown is required. Initiate if needed.
     */
    public void startSessionCountdownTimerIfNeeded() {
        if (_session == null) {
            return;
        }
        if (_session.isValid() && _sessionWillExpireIn > 0) {
            // Session is set to expire.
            final long timeUntilSessionExpires = _sessionWillExpireIn - System.currentTimeMillis();
            GigyaLogger.debug(LOG_TAG, "startSessionCountdownTimerIfNeeded: Session is set to expire in: "
                    + (timeUntilSessionExpires / 1000) + " start countdown timer");
            // Just in case.
            if (timeUntilSessionExpires > 0) {
                startSessionCountdown(timeUntilSessionExpires);
            }
        }
    }

    /**
     * Initiate session expiration countdown.
     * When finished. A local broadcast will be triggered.
     *
     * @param future Number of milliseconds to count down.
     */
    private void startSessionCountdown(long future) {
        cancelSessionCountdownTimer();
        _sessionLifeCountdownTimer = new CountDownTimer(future, TimeUnit.SECONDS.toMillis(1)) {
            @Override
            public void onTick(long millisUntilFinished) {
                // KEEP THIS LOG COMMENTED TO AVOID SPAMMING LOG_CAT!!!!!
                GigyaLogger.debug(LOG_TAG, "startSessionCountdown: Seconds remaining until session will expire = " + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                GigyaLogger.debug(LOG_TAG, "startSessionCountdown: Session expiration countdown done! Session is invalid");
                _persistenceService.setSessionExpiration(_sessionWillExpireIn = 0);
                // Send "session expired" local broadcast.
                LocalBroadcastManager.getInstance(_appContext).sendBroadcast(new Intent(GigyaDefinitions.Broadcasts.INTENT_ACTION_SESSION_EXPIRED));
            }
        }.start();
    }

    //endregion
}
