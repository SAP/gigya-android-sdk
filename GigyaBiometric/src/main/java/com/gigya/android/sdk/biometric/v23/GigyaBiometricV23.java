package com.gigya.android.sdk.biometric.v23;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.biometric.GigyaBiometric;
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;

import javax.crypto.SecretKey;

public class GigyaBiometricV23 extends GigyaBiometric {

    private static final String LOG_TAG = "GigyaBiometricV23";


    public GigyaBiometricV23(@Nullable String title, @Nullable String subtitle, @Nullable String description) {
        super(title, subtitle, description);
    }

    @Override
    public void showPrompt(Context context, @NonNull IGigyaBiometricCallback callback, @NonNull final Runnable onAuthenticated) {
        final SecretKey key = getKey();
        if (key == null) {
            GigyaLogger.error(LOG_TAG, "Unable to generate secret key from KeyStore API");
            return;
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
            CancellationSignal signal = new CancellationSignal();
            dialog.setCancellationSignal(signal);
            // Authenticate.
            fingerprintManagerCompat.authenticate(_cryptoObject, 0, signal, new FingerprintManagerCompat.AuthenticationCallback() {
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
        }
    }
}
