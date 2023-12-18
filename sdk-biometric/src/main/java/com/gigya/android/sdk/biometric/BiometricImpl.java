package com.gigya.android.sdk.biometric;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaInterceptor;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.encryption.ISecureKey;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.persistence.PersistenceService;
import com.gigya.android.sdk.session.ISessionService;
import com.gigya.android.sdk.session.SessionInfo;
import com.gigya.android.sdk.utils.CipherUtils;
import com.gigya.android.sdk.utils.ObjectUtils;

import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public abstract class BiometricImpl implements IBiometricImpl {

    private static final String LOG_TAG = "BiometricImpl";

    /**
     * Keystore fingerprint Alias.
     */
    private static final String FINGERPRINT_KEY_NAME = "fingerprint";

    final protected Context _context;
    final private Config _config;
    final private ISessionService _sessionService;
    final private IPersistenceService _persistenceService;
    final protected ISecureKey _biometricKey;

    public BiometricImpl(Context context, Config config, ISessionService sessionService, IPersistenceService persistenceService) {
        _context = context;
        _config = config;
        _sessionService = sessionService;
        _persistenceService = persistenceService;
        _biometricKey = new BiometricKey(_persistenceService);
    }

    protected abstract void updateAnimationState(boolean state);

    //region SET SESSION

    /**
     * Biometric setSession implementation.
     * Differs from original by using the biometric cipher instead.
     *
     * @param cipher      Cipher instance according to current encryption type.
     * @param sessionInfo Session data
     * @throws Exception Multiple crypto optional exception may occur..
     */
    private void setSession(@Nullable Cipher cipher, @NonNull SessionInfo sessionInfo) throws Exception {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("sessionToken", sessionInfo.getSessionToken());
        jsonObject.put("sessionSecret", sessionInfo.getSessionSecret());
        jsonObject.put("expirationTime", sessionInfo.getExpirationTime());
        jsonObject.put("ucid", _config.getUcid());
        jsonObject.put("gmid", _config.getGmid());
        final String plain = jsonObject.toString();
        // Encrypt.
        if (cipher == null) {
            final SecretKey key = _biometricKey.getKey();
            cipher = _biometricKey.getEncryptionCipher(key);
        }
        Pair<String, String> encodedPair = encryptBiometricString(cipher, plain);
        // Persist.
        _persistenceService.add(PersistenceService.PREFS_KEY_SESSION, encodedPair.first);
        _persistenceService.add(PersistenceService.PREFS_KEY_IV_SPEC, encodedPair.second);
        _persistenceService.add(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE, GigyaDefinitions.SessionEncryption.FINGERPRINT);
    }

    //endregion

    //region CONDITIONS

    /**
     * Check conditions for Opt-In/Out
     */
    boolean okayToOptInOut() {
        return (_sessionService != null && _sessionService.isValid());
    }

    /**
     * Check Opt-In state.
     */
    boolean isOptIn() {
        final String encryptionType = _persistenceService.getString(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE,
                GigyaDefinitions.SessionEncryption.DEFAULT);
        final boolean optIn = ObjectUtils.safeEquals(encryptionType, GigyaDefinitions.SessionEncryption.FINGERPRINT);
        GigyaLogger.debug(LOG_TAG, "isOptIn : " + String.valueOf(optIn));
        return optIn;
    }

    /**
     * Check locked state.
     */
    boolean isLocked() {
        return !_sessionService.isValid() && isOptIn();
    }

    //endregion

    //region OPERATIONS

    /**
     * Perform relevant success logic on successful biometric authentication.
     *
     * @param cipher            The current operation cipher instance.
     * @param action            Desired action.
     * @param biometricCallback Status callback.
     */
    synchronized protected void onSuccessfulAuthentication(
            final Cipher cipher,
            final GigyaBiometric.Action action,
            @NonNull final IGigyaBiometricCallback biometricCallback) {
        switch (action) {
            case OPT_IN:
                optIn(cipher, biometricCallback);
                break;
            case OPT_OUT:
                optOut(cipher, biometricCallback);
                break;
            case UNLOCK:
                unlock(cipher, biometricCallback);
                break;
            default:
                break;
        }
    }

    /**
     * Opt-In operation.
     * Encrypt the current persistent session with the fingerprint key.
     *
     * @param biometricCallback Status callback.
     */
    private void optIn(@NonNull Cipher cipher, @NonNull final IGigyaBiometricCallback biometricCallback) {
        final String encryptionType = _persistenceService.getString(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE,
                GigyaDefinitions.SessionEncryption.DEFAULT);
        if (encryptionType.equals(GigyaDefinitions.SessionEncryption.FINGERPRINT)) {
            GigyaLogger.error(LOG_TAG, "Fingerprint already opt-in");
            return;
        }
        final SessionInfo sessionInfo = _sessionService.getSession();
        if (sessionInfo == null) {
            GigyaLogger.error(LOG_TAG, "Session is null Opt-In failed");
            return;
        }
        try {
            setSession(cipher, sessionInfo);
            biometricCallback.onBiometricOperationSuccess(GigyaBiometric.Action.OPT_IN);
        } catch (Exception ex) {
            ((BiometricKey) _biometricKey).deleteKey();
            ex.printStackTrace();
            biometricCallback.onBiometricOperationFailed("Fingerprint optIn: " + ex.getMessage());
        }
    }

    /**
     * Opt-Out operation.
     * Set current heap session. Save as default encryption.
     *
     * @param biometricCallback Status callback.
     */
    private void optOut(@NonNull Cipher cipher, IGigyaBiometricCallback biometricCallback) {
        final String encryptionType = _persistenceService.getString(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE,
                GigyaDefinitions.SessionEncryption.DEFAULT);
        if (encryptionType.equals(GigyaDefinitions.SessionEncryption.DEFAULT)) {
            GigyaLogger.error(LOG_TAG, "Fingerprint already opt-out");
            return;
        }
        try {
            final SessionInfo sessionInfo = decryptBiometricSession(cipher);
            // Reset session encryption to DEFAULT.
            _persistenceService.add(PersistenceService.PREFS_KEY_SESSION_ENCRYPTION_TYPE, GigyaDefinitions.SessionEncryption.DEFAULT);
            _sessionService.setSession(sessionInfo);
            // Delete KeyStore.
            ((BiometricKey) _biometricKey).deleteKey();
            biometricCallback.onBiometricOperationSuccess(GigyaBiometric.Action.OPT_OUT);
        } catch (Exception ex) {
            ex.printStackTrace();
            biometricCallback.onBiometricOperationFailed("Fingerprint optOut: " + ex.getMessage());
        }
    }

    /**
     * Lock operation.
     * Clear current heap session. Does not require biometric authentication.
     *
     * @param biometricCallback Status callback.
     */
    void lock(IGigyaBiometricOperationCallback biometricCallback) {
        // Locking means clearing the SessionInfo from the heap but keeping the persistence.
        _sessionService.clear(false);
        biometricCallback.onBiometricOperationSuccess(GigyaBiometric.Action.LOCK);
    }

    /**
     * Unlock operation.
     * Decrypt fingerprint session and save as default.
     *
     * @param biometricCallback Status callback.
     */
    private void unlock(@NonNull Cipher cipher, IGigyaBiometricCallback biometricCallback) {
        try {
            // Decrypt the session.
            final SessionInfo sessionInfo = decryptBiometricSession(cipher);
            _sessionService.setSession(sessionInfo);
            // Refresh expiration. If any.
            _sessionService.refreshSessionExpiration();
            biometricCallback.onBiometricOperationSuccess(GigyaBiometric.Action.UNLOCK);
        } catch (Exception ex) {
            ex.printStackTrace();
            biometricCallback.onBiometricOperationFailed("Fingerprint unlock: " + ex.getMessage());
        }
    }

    //endregion

    //region ENCRYPTION DECRYPTION

    /**
     * Encrypt biometric session JSON string given a cipher.
     *
     * @param cipher Encryption state cipher.
     * @param plain  Plain JSON string session.
     * @return Pair of encrypted session and ivSpec.
     * @throws Exception Various crypto optional exceptions.
     */
    private Pair<String, String> encryptBiometricString(Cipher cipher, String plain) throws Exception {
        final byte[] encryptedBytes = cipher.doFinal(CipherUtils.toBytes(plain.toCharArray()));
        final byte[] ivBytes = cipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
        final String encodedSession = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        final String encodedIVSpec = Base64.encodeToString(ivBytes, Base64.DEFAULT);
        return new Pair<>(encodedSession, encodedIVSpec);
    }

    /**
     * Decrypt biometric session from storage.
     *
     * @param cipher Decryption state cipher.
     * @return SessionInfo instance of decrypted session.
     * @throws Exception Various crypto optional exceptions.
     */
    private SessionInfo decryptBiometricSession(Cipher cipher) throws Exception {
        final String encryptedSession = _persistenceService.getString(PersistenceService.PREFS_KEY_SESSION, null);
        // Decrypt & set the session.
        byte[] encPLBytes = Base64.decode(encryptedSession, Base64.DEFAULT);
        byte[] bytePlainText = cipher.doFinal(encPLBytes);
        final String plain = new String(CipherUtils.toChars(bytePlainText));
        // Create JSON from plain String and fetch fields.
        JSONObject jsonObject = new JSONObject(plain);
        final String sessionToken = jsonObject.has("sessionToken") ? jsonObject.getString("sessionToken") : null;
        final String sessionSecret = jsonObject.has("sessionSecret") ? jsonObject.getString("sessionSecret") : null;
        final long expirationTime = jsonObject.has("expirationTime") ? jsonObject.getLong("expirationTime") : 0L;
        // Updating config.
        final String ucid = jsonObject.getString("ucid");
        _config.setUcid(ucid);
        final String gmid = jsonObject.getString("gmid");
        _config.setGmid(gmid);
        // Return.
        return new SessionInfo(sessionSecret, sessionToken, expirationTime);
    }

    //endregion

    /**
     * Key permanently invalidated.
     * OS security state has changed. Session is not recoverable.
     * Key needs to be deleted and session invalidated.
     */
    protected void onInvalidKey() {
        _sessionService.clear(true);
        ((BiometricKey) _biometricKey).deleteKey();
    }
}
