package com.gigya.android.sdk.biometric;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.services.PersistenceService;
import com.gigya.android.sdk.services.SessionService;
import com.gigya.android.sdk.utils.CipherUtils;
import com.gigya.android.sdk.utils.ObjectUtils;

import org.json.JSONObject;

import javax.crypto.SecretKey;

class BiometricSessionHandler {

    final private SessionService _sessionService;

    private static final String LOG_TAG = "SessionService";

    /*
    Session info encryption algorithm.
     */
    private static final String ENCRYPTION_ALGORITHM = "AES";

    BiometricSessionHandler(SessionService sessionService) {
        _sessionService = sessionService;
    }

    boolean okayToOptInOut() {
        return (_sessionService != null && _sessionService.isValidSession());
    }

    boolean isOptIn() {
        final String encryptionType = _sessionService.getPersistenceService().getSessionEncryption();
        final boolean optIn = ObjectUtils.safeEquals(encryptionType, SessionService.FINGERPRINT);
        GigyaLogger.debug(LOG_TAG, "isOptIn : " + String.valueOf(optIn));
        return optIn;
    }

    /**
     * Opt-In operation.
     * Encrypt the current persistent session with the fingerprint key.
     *
     * @param key               SecretKey.
     * @param biometricCallback Status callback.
     */
    void optIn(final SecretKey key, @NonNull final IGigyaBiometricCallback biometricCallback) {
        final PersistenceService persistenceService = _sessionService.getPersistenceService();
        final String encryptionType = persistenceService.getSessionEncryption();
        if (encryptionType.equals(SessionService.FINGERPRINT)) {
            GigyaLogger.error(LOG_TAG, "Fingerprint already opt-in");
            return;
        }
        final SessionInfo sessionInfo = _sessionService.getSession();
        if (sessionInfo == null) {
            GigyaLogger.error(LOG_TAG, "Session is null Opt-In failed");
            return;
        }
        final JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("sessionToken", sessionInfo.getSessionToken());
            jsonObject.put("sessionSecret", sessionInfo.getSessionSecret());
            jsonObject.put("expirationTime", sessionInfo.getExpirationTime());
            jsonObject.put("ucid", _sessionService.getConfig().getUcid());
            jsonObject.put("gmid", _sessionService.getConfig().getGmid());

            final String plain = jsonObject.toString();
            final String encrypted = CipherUtils.encrypt(plain, ENCRYPTION_ALGORITHM, key);
            persistenceService.setSession(encrypted);
            persistenceService.updateSessionEncryption(SessionService.FINGERPRINT);
            // Callback.
            biometricCallback.onBiometricOperationSuccess();
        } catch (Exception ex) {
            ex.printStackTrace();
            // Callback.
            biometricCallback.onBiometricOperationFailed("Fingerprint optIn: " + ex.getMessage());
        }
    }

    /**
     * Opt-Out operation.
     * Set current heap session. Save as default encryption.
     *
     * @param biometricCallback Status callback.
     */
    void optOut(IGigyaBiometricCallback biometricCallback) {
        final PersistenceService persistenceService = _sessionService.getPersistenceService();
        final String encryptionType = persistenceService.getSessionEncryption();
        if (encryptionType.equals(SessionService.FINGERPRINT)) {
            GigyaLogger.error(LOG_TAG, "Fingerprint already opt-in");
            return;
        }
        final SessionInfo sessionInfo = _sessionService.getSession();
        // Set the session (DEFAULT encryption).
        _sessionService.setSession(sessionInfo);
        // Callback.
        biometricCallback.onBiometricOperationSuccess();
    }

    /**
     * Lock operation.
     * Clear current heap session.
     *
     * @param biometricCallback Status callback.
     */
    void lock(IGigyaBiometricCallback biometricCallback) {
        // Locking means clearing the SessionInfo from the heap but keeping the persistence.
        _sessionService.clear(false);
        // Callback.
        biometricCallback.onBiometricOperationSuccess();
    }

    /**
     * Unlock operation.
     * Decrypt fingerprint session and save as default.
     *
     * @param key               SecretKey.
     * @param biometricCallback Status callback.
     */
    void unlock(SecretKey key, IGigyaBiometricCallback biometricCallback) {
        final PersistenceService persistenceService = _sessionService.getPersistenceService();
        final String encryptedSession = persistenceService.getSession();
        // Decrypt the session.
        try {
            // Decrypt & set the session.
            final String plain = CipherUtils.decrypt(encryptedSession, ENCRYPTION_ALGORITHM, key);
            JSONObject jsonObject = new JSONObject(plain);
            final String sessionToken = jsonObject.has("sessionToken") ? jsonObject.getString("sessionToken") : null;
            final String sessionSecret = jsonObject.has("sessionSecret") ? jsonObject.getString("sessionSecret") : null;
            final long expirationTime = jsonObject.has("expirationTime") ? jsonObject.getLong("expirationTime") : -1;
            final SessionInfo sessionInfo = new SessionInfo(sessionSecret, sessionToken, expirationTime);
            final String ucid = jsonObject.getString("ucid");
            _sessionService.getConfig().setUcid(ucid);
            final String gmid = jsonObject.getString("gmid");
            _sessionService.getConfig().setGmid(gmid);
            // Set the session (DEFAULT encryption).
            _sessionService.setSession(sessionInfo);
            _sessionService.updateSessionExpirationIfExists();
            // Callback.
            biometricCallback.onBiometricOperationSuccess();
        } catch (Exception ex) {
            ex.printStackTrace();
            biometricCallback.onBiometricOperationFailed("Fingerprint unlock: " + ex.getMessage());
        }

    }

}
