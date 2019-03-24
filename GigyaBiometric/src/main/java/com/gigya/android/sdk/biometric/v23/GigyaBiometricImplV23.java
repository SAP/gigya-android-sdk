package com.gigya.android.sdk.biometric.v23;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.GigyaBiometric;
import com.gigya.android.sdk.biometric.GigyaBiometricImpl;
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;
import com.gigya.android.sdk.biometric.model.GigyaPromptInfo;
import com.gigya.android.sdk.services.SessionService;

public class GigyaBiometricImplV23 extends GigyaBiometricImpl {

    private static final String LOG_TAG = "GigyaBiometricImplV23";


    public GigyaBiometricImplV23(SessionService sessionService) {
        super(sessionService);
    }

    @Override
    public void showPrompt(Context context, GigyaBiometric.Action action, @NonNull GigyaPromptInfo gigyaPromptInfo, int encryptionMode, @NonNull IGigyaBiometricCallback callback,
                           @NonNull final Runnable onAuthenticated) {
        getKey();
        if (_secretKey == null) {
            GigyaLogger.error(LOG_TAG, "Unable to generate secret key from KeyStore API");
            return;
        }
        createCipherFor(encryptionMode);
        if (_cipher != null) {
            // Init crypto.
            final FingerprintManagerCompat.CryptoObject cryptoObject = new FingerprintManagerCompat.CryptoObject(_cipher);
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
                    onAuthenticated.run();
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
