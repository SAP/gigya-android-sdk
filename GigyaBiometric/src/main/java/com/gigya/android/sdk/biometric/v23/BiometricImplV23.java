package com.gigya.android.sdk.biometric.v23;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.BiometricImpl;
import com.gigya.android.sdk.biometric.GigyaBiometric;
import com.gigya.android.sdk.biometric.GigyaPromptInfo;
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.ISessionService;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class BiometricImplV23 extends BiometricImpl {

    private static final String LOG_TAG = "BiometricImplV23";

    public BiometricImplV23(Context context, Config config, ISessionService sessionService, IPersistenceService persistenceService) {
        super(context, config, sessionService, persistenceService);
    }

    private boolean _animate = true;

    public void updateAnimationState(boolean animate) {
        _animate = animate;
    }

    @Override
    synchronized public void showPrompt(final GigyaBiometric.Action action, @NonNull GigyaPromptInfo gigyaPromptInfo,
                                        int encryptionMode, @NonNull final IGigyaBiometricCallback callback) {
        final SecretKey key = _biometricKey.getKey();
        if (key == null) {
            GigyaLogger.error(LOG_TAG, "Unable to generate secret key from KeyStore API");
            return;
        }
        final Cipher cipher;
        if (encryptionMode == Cipher.DECRYPT_MODE) {
            cipher = _biometricKey.getDecryptionCipher(key);
        } else {
            cipher = _biometricKey.getEncryptionCipher(key);
        }
        if (cipher != null) {
            // Init crypto.
            final FingerprintManagerCompat.CryptoObject cryptoObject = new FingerprintManagerCompat.CryptoObject(cipher);
            final FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(_context);
            // Initialize prompt dialog.
            final GigyaBiometricPromptV23 dialog = new GigyaBiometricPromptV23(_context, callback);
            dialog.setTitle(gigyaPromptInfo.getTitle() != null ? gigyaPromptInfo.getTitle() : _context.getString(R.string.prompt_default_title));
            dialog.setSubtitle(gigyaPromptInfo.getSubtitle() != null ? gigyaPromptInfo.getSubtitle() : _context.getString(R.string.prompt_default_subtitle));
            dialog.setDescription(gigyaPromptInfo.getDescription() != null ? gigyaPromptInfo.getDescription() : _context.getString(R.string.prompt_default_description));
            dialog.setAnimate(_animate);
            CancellationSignal signal = new CancellationSignal();
            dialog.setCancellationSignal(signal);
            // Authenticate.
            fingerprintManagerCompat.authenticate(cryptoObject, 0, signal, new FingerprintManagerCompat.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errMsgId, CharSequence errString) {
                    GigyaLogger.error(LOG_TAG, "onAuthenticationError: " + errString);
                    dialog.onAuthenticationError(errMsgId, errString.toString());
                }

                @Override
                public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                    GigyaLogger.debug(LOG_TAG, "onAuthenticationHelp: " + helpString);
                    dialog.onAuthenticationHelp(helpString.toString());
                }

                @Override
                public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                    GigyaLogger.debug(LOG_TAG, "onAuthenticationSucceeded: ");
                    onSuccessfulAuthentication(cipher, action, callback);
                    dialog.dismiss();
                }

                @Override
                public void onAuthenticationFailed() {
                    GigyaLogger.debug(LOG_TAG, "onAuthenticationFailed: ");
                    dialog.onAuthenticationFailed();
                }
            }, null);
            // Show biometric dialog.
            dialog.show();
        } else {
            //Error
            GigyaLogger.error(LOG_TAG, "Failed to initialize cipher");
        }
    }
}
