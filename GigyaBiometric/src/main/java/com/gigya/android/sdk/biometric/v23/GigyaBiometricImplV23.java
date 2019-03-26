package com.gigya.android.sdk.biometric.v23;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.GigyaBiometric;
import com.gigya.android.sdk.biometric.GigyaBiometricImpl;
import com.gigya.android.sdk.biometric.GigyaPromptInfo;
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;
import com.gigya.android.sdk.services.SessionService;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;

public class GigyaBiometricImplV23 extends GigyaBiometricImpl {

    private static final String LOG_TAG = "GigyaBiometricImplV23";


    public GigyaBiometricImplV23(SessionService sessionService) {
        super(sessionService);
    }

    @Override
    synchronized public void showPrompt(Context context, final GigyaBiometric.Action action, @NonNull GigyaPromptInfo gigyaPromptInfo,
                                        int encryptionMode, @NonNull final IGigyaBiometricCallback callback) {
        final SecretKey key = getKey();
        if (key == null) {
            GigyaLogger.error(LOG_TAG, "Unable to generate secret key from KeyStore API");
            return;
        }
        final Cipher cipher = createCipherFor(key, encryptionMode);
        if (cipher != null) {
            // Init crypto.
            final FingerprintManagerCompat.CryptoObject cryptoObject = new FingerprintManagerCompat.CryptoObject(cipher);
            final FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(context);
            // Initialize prompt dialog.
            final GigyaBiometricPromptV23 dialog = new GigyaBiometricPromptV23(context, callback);
            dialog.setTitle(gigyaPromptInfo.getTitle() != null ? gigyaPromptInfo.getTitle() : context.getString(R.string.prompt_default_title));
            dialog.setSubtitle(gigyaPromptInfo.getSubtitle() != null ? gigyaPromptInfo.getSubtitle() : context.getString(R.string.prompt_default_subtitle));
            dialog.setDescription(gigyaPromptInfo.getDescription() != null ? gigyaPromptInfo.getDescription() : context.getString(R.string.prompt_default_description));
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
        }
    }
}
