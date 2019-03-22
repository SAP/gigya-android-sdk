package com.gigya.android.sdk.biometric;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.utils.GigyaBiometricUtils;
import com.gigya.android.sdk.biometric.v23.GigyaBiometricV23;
import com.gigya.android.sdk.biometric.v28.GigyaBiometricV28;
import com.gigya.android.sdk.services.SessionService;

import java.security.KeyStore;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


public abstract class GigyaBiometric implements IGigyaBiometricActions {

    private static final String LOG_TAG = "GigyaBiometric";

    protected static final String FINGERPRINT_KEY_NAME = "fingerprint";

    @Nullable
    protected String title, subtitle, description;

    private BiometricSessionHandler _sessionHandler;

    public GigyaBiometric(@Nullable String title, @Nullable String subtitle, @Nullable String description) {
        this.title = title;
        this.subtitle = subtitle;
        this.description = description;

        Gigya gigya = Gigya.getInstance();
        if (gigya == null) {
            GigyaLogger.error(LOG_TAG, "Gigya interface null. Please make sure" +
                    " to correctly instantiate the Gigya SDK within your main application class");
            return;
        }
        // Check support conditions.
        final String verificationMessage = verifyBiometricSupport(gigya);
        if (verificationMessage != null) {
            return;
        }
        // Reference session service.
        final SessionService sessionService = (SessionService) gigya.getGigyaComponent(SessionService.class);
        _sessionHandler = new BiometricSessionHandler(sessionService);
    }

    /**
     * Verify that all needed conditions are available to use biometric operation.
     */
    @Nullable
    private String verifyBiometricSupport(Gigya gigya) {
        String message = null;
        if (!GigyaBiometricUtils.isSupported(gigya.getContext())) {
            message = "Fingerprint is not supported on this device. No sensor hardware was detected";
            GigyaLogger.error(LOG_TAG, message);
        }
        if (!GigyaBiometricUtils.hasEnrolledFingerprints(gigya.getContext())) {
            message = "No fingerprint data available on device. Please enroll at least one fingerprint";
            GigyaLogger.error(LOG_TAG, message);
        }
        return message;
    }

    //region BIOMETRIC OPERATIONS

    public void optIn(Context context, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "optIn: ");
        if (_sessionHandler.okayToOptInOut()) {
            showPrompt(context, biometricCallback, new Runnable() {
                @Override
                public void run() {
                    _sessionHandler.optIn(getKey(), biometricCallback);
                }
            });
        } else {
            GigyaLogger.error(LOG_TAG, "Session is invalid. Opt in operation is unavailable");
        }
    }

    public void optOut(Context context, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "optOut: ");
        if (_sessionHandler.okayToOptInOut()) {
            showPrompt(context, biometricCallback, new Runnable() {
                @Override
                public void run() {
                    _sessionHandler.optOut(biometricCallback);
                }
            });
        } else {
            GigyaLogger.error(LOG_TAG, "Session is invalid. Opt in operation is unavailable");
        }
    }

    public void lock(Context context, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "lock: ");
        if (_sessionHandler.isOptIn()) {
            showPrompt(context, biometricCallback, new Runnable() {
                @Override
                public void run() {
                    // Lock the session.
                    _sessionHandler.lock(biometricCallback);
                }
            });
        } else {
            GigyaLogger.error(LOG_TAG, "Not Opt-in");
        }
    }

    public void unlock(Context context, final IGigyaBiometricCallback biometricCallback) {
        GigyaLogger.debug(LOG_TAG, "unlock: ");
        if (_sessionHandler.isOptIn()) {
            showPrompt(context, biometricCallback, new Runnable() {
                @Override
                public void run() {
                    // Unlock the session.
                    _sessionHandler.unlock(getKey(), biometricCallback);
                }
            });
        } else {
            GigyaLogger.error(LOG_TAG, "Not Opt-in");
        }
    }

    //endregion

    //region KEYSTORE

    private KeyStore _keyStore;
    protected Cipher _cipher;

    @Nullable
    protected SecretKey getKey() {
        try {
            _keyStore = KeyStore.getInstance("AndroidKeyStore");
            _keyStore.load(null);

            final Enumeration<String> aliases = _keyStore.aliases();
            while (aliases.hasMoreElements()) {
                if (FINGERPRINT_KEY_NAME.equals(aliases.nextElement())) {
                    return (SecretKey) _keyStore.getKey(FINGERPRINT_KEY_NAME, null);
                }
            }
            final KeyGenerator _keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            _keyGenerator.init(new
                    KeyGenParameterSpec.Builder(FINGERPRINT_KEY_NAME, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            return _keyGenerator.generateKey();
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    protected boolean initializeCipher() {
        try {
            Cipher _cipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            _keyStore.load(null);
            final SecretKey key = (SecretKey) _keyStore.getKey(FINGERPRINT_KEY_NAME,
                    null);
            _cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //endregion

    //region BUILDER

    public static class Builder {

        @Nullable
        private String title;
        @Nullable
        private String subtitle;
        @Nullable
        private String description;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder subtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        @NonNull
        public GigyaBiometric build() {
            if (GigyaBiometricUtils.isPromptEnabled()) {
                return new GigyaBiometricV28(title, subtitle, description);
            } else {
                return new GigyaBiometricV23(title, subtitle, description);
            }
        }
    }

    //endregion
}
