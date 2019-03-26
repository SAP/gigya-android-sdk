package com.gigya.android.sdk.biometric;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.util.Base64;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.model.GigyaInterceptor;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.services.PersistenceService;
import com.gigya.android.sdk.services.SessionService;
import com.gigya.android.sdk.utils.CipherUtils;
import com.gigya.android.sdk.utils.ObjectUtils;

import org.json.JSONObject;

import java.security.KeyStore;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;


public abstract class GigyaBiometricImpl implements IGigyaBiometricActions {

    private static final String LOG_TAG = "GigyaBiometricImpl";

    private static final String FINGERPRINT_KEY_NAME = "fingerprint";

    final private SessionService _sessionService;

    public GigyaBiometricImpl(SessionService sessionService) {
        // Reference session service.
        _sessionService = sessionService;
        addSessionInterception();
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

    boolean isLocked() {
        return !_sessionService.isValidSession() && isOptIn();
    }

    private void addSessionInterception() {
        _sessionService.addInterceptor(new GigyaInterceptor("BIOMETRIC") {
            @Override
            public void intercept() {
                final SessionInfo sessionInfo = _sessionService.getSession();
                if (sessionInfo == null) {
                    GigyaLogger.error(LOG_TAG, "Session is null cannot set the session");
                    return;
                }
                try {
                    setSession(null, _sessionService.getSession());
                } catch (Exception e) {
                    GigyaLogger.error(LOG_TAG, "Error setting biometric session");
                    e.printStackTrace();
                }
            }
        });
    }

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
            case LOCK:
                lock(biometricCallback);
                break;
            case UNLOCK:
                unlock(cipher, biometricCallback);
                break;
            default:
                break;
        }
    }

    private void setSession(@Nullable Cipher cipher, @NonNull SessionInfo sessionInfo) throws Exception {
        final PersistenceService persistenceService = _sessionService.getPersistenceService();
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("sessionToken", sessionInfo.getSessionToken());
        jsonObject.put("sessionSecret", sessionInfo.getSessionSecret());
        jsonObject.put("expirationTime", sessionInfo.getExpirationTime());
        jsonObject.put("ucid", _sessionService.getConfig().getUcid());
        jsonObject.put("gmid", _sessionService.getConfig().getGmid());
        final String plain = jsonObject.toString();
        // Encrypt.
        if (cipher == null) {
            cipher = createCipherFor(getKey(), Cipher.ENCRYPT_MODE);
        }
        Pair<String, String> encodedPair = encryptBiometricString(cipher, plain);
        // Persist.
        persistenceService.setSession(encodedPair.first);
        persistenceService.updateIVSpec(encodedPair.second);
        persistenceService.updateSessionEncryption(SessionService.FINGERPRINT);
    }

    /**
     * Opt-In operation.
     * Encrypt the current persistent session with the fingerprint key.
     *
     * @param biometricCallback Status callback.
     */
    private void optIn(@NonNull Cipher cipher, @NonNull final IGigyaBiometricCallback biometricCallback) {
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
        try {
            setSession(cipher, sessionInfo);
            biometricCallback.onBiometricOperationSuccess(GigyaBiometric.Action.OPT_IN);
        } catch (Exception ex) {
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
        final PersistenceService persistenceService = _sessionService.getPersistenceService();
        final String encryptionType = persistenceService.getSessionEncryption();
        if (encryptionType.equals(SessionService.DEFAULT)) {
            GigyaLogger.error(LOG_TAG, "Fingerprint already opt-out");
            return;
        }
        try {
            final SessionInfo sessionInfo = decryptBiometricSession(cipher);
            // Reset session encryption to DEFAULT.
            _sessionService.getPersistenceService().updateSessionEncryption(SessionService.DEFAULT);
            _sessionService.setSession(sessionInfo);
            // Delete KeyStore.
            deleteKey();
            biometricCallback.onBiometricOperationSuccess(GigyaBiometric.Action.OPT_OUT);
        } catch (Exception ex) {
            ex.printStackTrace();
            biometricCallback.onBiometricOperationFailed("Fingerprint optOut: " + ex.getMessage());
        }
    }

    /**
     * Lock operation.
     * Clear current heap session.
     *
     * @param biometricCallback Status callback.
     */
    private void lock(IGigyaBiometricCallback biometricCallback) {
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
            _sessionService.updateSessionExpirationIfExists();
            biometricCallback.onBiometricOperationSuccess(GigyaBiometric.Action.UNLOCK);
        } catch (Exception ex) {
            ex.printStackTrace();
            biometricCallback.onBiometricOperationFailed("Fingerprint unlock: " + ex.getMessage());
        }
    }

    private Pair<String, String> encryptBiometricString(Cipher cipher, String plain) throws Exception {
        final byte[] encryptedBytes = cipher.doFinal(CipherUtils.toBytes(plain.toCharArray()));
        final byte[] ivBytes = cipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
        final String encodedSession = Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        final String encodedIVSpec = Base64.encodeToString(ivBytes, Base64.DEFAULT);
        return new Pair<>(encodedSession, encodedIVSpec);
    }

    private SessionInfo decryptBiometricSession(Cipher cipher) throws Exception {
        final PersistenceService persistenceService = _sessionService.getPersistenceService();
        final String encryptedSession = persistenceService.getSession();
        // Decrypt & set the session.
        byte[] encPLBytes = Base64.decode(encryptedSession, Base64.DEFAULT);
        byte[] bytePlainText = cipher.doFinal(encPLBytes);
        final String plain = new String(CipherUtils.toChars(bytePlainText));
        JSONObject jsonObject = new JSONObject(plain);
        final String sessionToken = jsonObject.has("sessionToken") ? jsonObject.getString("sessionToken") : null;
        final String sessionSecret = jsonObject.has("sessionSecret") ? jsonObject.getString("sessionSecret") : null;
        final long expirationTime = jsonObject.has("expirationTime") ? jsonObject.getLong("expirationTime") : -1;
        // Updating CMID/UCID.
        final String ucid = jsonObject.getString("ucid");
        _sessionService.getConfig().setUcid(ucid);
        final String gmid = jsonObject.getString("gmid");
        _sessionService.getConfig().setGmid(gmid);
        // Return.
        return new SessionInfo(sessionSecret, sessionToken, expirationTime);
    }

    //region KEYSTORE

    private void deleteKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.deleteEntry(FINGERPRINT_KEY_NAME);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Get or generate new SecretKey.
     */
    @Nullable
    protected SecretKey getKey() {
        try {
            KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
            keyStore.load(null);

            final Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                if (FINGERPRINT_KEY_NAME.equals(aliases.nextElement())) {
                    try {
                        return (SecretKey) keyStore.getKey(FINGERPRINT_KEY_NAME, null);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        deleteKey();
                    }
                }
            }
            final KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            keyGenerator.init(new
                    KeyGenParameterSpec.Builder(FINGERPRINT_KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setUserAuthenticationRequired(true)
                    .setKeySize(256)
                    .build());
            return keyGenerator.generateKey();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /**
     * Create cipher instance according to desired encryption mode.
     *
     * @param encryptionMode Cipher encryption mode (Cipher.ENCRYPT_MODE/Cipher.DECRYPT_MODE).
     */
    protected Cipher createCipherFor(final SecretKey key, final int encryptionMode) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            final PersistenceService persistenceService = _sessionService.getPersistenceService();
            if (encryptionMode == Cipher.ENCRYPT_MODE) {
                cipher.init(encryptionMode, key);
            } else if (encryptionMode == Cipher.DECRYPT_MODE) {
                final String ivSpec = persistenceService.getIVSpec();
                if (ivSpec != null) {
                    final IvParameterSpec spec = new IvParameterSpec(Base64.decode(ivSpec, Base64.DEFAULT));
                    cipher.init(Cipher.DECRYPT_MODE, key, spec);
                } else {
                    GigyaLogger.error(LOG_TAG, "createCipherFor: getIVSpec null");
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //endregion
}
