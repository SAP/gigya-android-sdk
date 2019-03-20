package com.gigya.android.sdk.biometric.v23;

import android.annotation.SuppressLint;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.Nullable;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.IBiometricHalder;

import java.security.KeyStore;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class GigyaBiometricV23 implements IBiometricHalder {

    private static final String LOG_TAG = "GigyaBiometricV23";

    private static final String FINGERPRINT_KEY_NAME = "fingerprint";

    private Cipher _cipher;
    private KeyStore _keyStore;
    private FingerprintManagerCompat.CryptoObject _cryptoObject;

    @SuppressLint("MissingPermission")
    @Override
    public void showPrompt(Context context) {
        SecretKey key = getKey();
        if (key == null) {
            GigyaLogger.error(LOG_TAG, "Unable to generate secret key from KeyStore API");
            return;
        }
        if (initializeCipher()) {
            _cryptoObject = new FingerprintManagerCompat.CryptoObject(_cipher);
            final FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(context);
            fingerprintManagerCompat.authenticate(_cryptoObject, 0, new CancellationSignal(), new FingerprintManagerCompat.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errMsgId, CharSequence errString) {
                    GigyaLogger.error(LOG_TAG, "onAuthenticationError: " + errString);
                }

                @Override
                public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                    GigyaLogger.debug(LOG_TAG, "onAuthenticationHelp: " + helpString);
                }

                @Override
                public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                    GigyaLogger.debug(LOG_TAG, "onAuthenticationSucceeded: ");
                }

                @Override
                public void onAuthenticationFailed() {
                    GigyaLogger.debug(LOG_TAG, "onAuthenticationFailed: ");
                }
            }, null);
            // Show biometric dialog.
            displayDialog();
        }
    }

    @Override
    public void dismiss() {

    }

    private void displayDialog() {

    }

    @Nullable
    private SecretKey getKey() {
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

    private boolean initializeCipher() {
        try {
            _cipher = Cipher.getInstance(
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
}
