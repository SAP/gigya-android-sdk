package com.gigya.android.sdk.biometric.v28;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.biometric.GigyaBiometric;
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;
import com.gigya.android.sdk.biometric.R;

@TargetApi(Build.VERSION_CODES.P)
public class GigyaBiometricV28 extends GigyaBiometric {

    private static final String LOG_TAG = "GigyaBiometricV28";

    public GigyaBiometricV28(@Nullable String title, @Nullable String subtitle, @Nullable String description) {
        super(title, subtitle, description);
    }

    @Override
    public void showPrompt(Context context, final @NonNull IGigyaBiometricCallback callback, @NonNull final Runnable onAuthenticated) {
        BiometricPrompt prompt = new BiometricPrompt.Builder(context)
                .setTitle(title != null ? title : context.getString(R.string.prompt_default_title))
                .setSubtitle(subtitle != null ? subtitle : context.getString(R.string.prompt_default_subtitle))
                .setDescription(description != null ? description : context.getString(R.string.prompt_default_description))
                .setNegativeButton("Cancel", context.getMainExecutor(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .build();
        final BiometricPrompt.CryptoObject _cryptoObject = new BiometricPrompt.CryptoObject(_cipher);
        prompt.authenticate(_cryptoObject, new CancellationSignal(), context.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
            }

            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                onAuthenticated.run();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                callback.onBiometricOperationCanceled();
            }
        });
    }

}
