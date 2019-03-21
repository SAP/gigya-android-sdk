package com.gigya.android.sdk.biometric.v23;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.GigyaBiometric;
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;

import java.security.KeyStore;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class GigyaBiometricV23 extends GigyaBiometric {

    private static final String LOG_TAG = "GigyaBiometricV23";

    private Cipher _cipher;
    private KeyStore _keyStore;

    public GigyaBiometricV23(@Nullable String title, @Nullable String subtitle, @Nullable String description) {
        super(title, subtitle, description);
    }

    @Override
    public void showPrompt(Context context, @NonNull IGigyaBiometricCallback callback, @NonNull final Runnable onAuthenticated) {
        SecretKey key = getKey();
        if (key == null) {
            GigyaLogger.error(LOG_TAG, "Unable to generate secret key from KeyStore API");
            //return;
        }
        if (initializeCipher()) {
            // Init crypto.
            final FingerprintManagerCompat.CryptoObject _cryptoObject = new FingerprintManagerCompat.CryptoObject(_cipher);
            final FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(context);
            // Initialize prompt dialog.
            final GigyaBiometricPromptV23 dialog = new GigyaBiometricPromptV23(context, callback);
            dialog.setTitle(title != null ? title : context.getString(R.string.prompt_default_title));
            dialog.setSubtitle(subtitle != null ? subtitle : context.getString(R.string.prompt_default_subtitle));
            dialog.setDescription(description != null ? description : context.getString(R.string.prompt_default_description));
            // Authenticate.
            fingerprintManagerCompat.authenticate(_cryptoObject, 0, new CancellationSignal(), new FingerprintManagerCompat.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errMsgId, CharSequence errString) {
                    GigyaLogger.error(LOG_TAG, "onAuthenticationError: " + errString);
                    dialog.onAuthenticationError(errString.toString());
                }

                @Override
                public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                    GigyaLogger.debug(LOG_TAG, "onAuthenticationHelp: " + helpString);
                    dialog.onAuthenticationHelp(helpString.toString());
                }

                @Override
                public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
                    GigyaLogger.debug(LOG_TAG, "onAuthenticationSucceeded: ");
                    onAuthenticated.run();
                    dialog.dismiss();
                }

                @Override
                public void onAuthenticationFailed() {
                    GigyaLogger.debug(LOG_TAG, "onAuthenticationFailed: ");
                    // TODO: 21/03/2019 Hmmmm.
                }
            }, null);
            // Show biometric dialog.
            dialog.show();
        }
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
        return true;
//        try {
//            _cipher = Cipher.getInstance(
//                    KeyProperties.KEY_ALGORITHM_AES + "/"
//                            + KeyProperties.BLOCK_MODE_CBC + "/"
//                            + KeyProperties.ENCRYPTION_PADDING_PKCS7);
//            _keyStore.load(null);
//            final SecretKey key = (SecretKey) _keyStore.getKey(FINGERPRINT_KEY_NAME,
//                    null);
//            _cipher.init(Cipher.ENCRYPT_MODE, key);
//            return true;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
    }
}
