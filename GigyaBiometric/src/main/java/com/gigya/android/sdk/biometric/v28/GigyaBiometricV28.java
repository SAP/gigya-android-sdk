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
import com.gigya.android.sdk.biometric.IBiometricActions;
import com.gigya.android.sdk.biometric.IGigyaBiometricCallback;

@TargetApi(Build.VERSION_CODES.P)
public class GigyaBiometricV28 extends GigyaBiometric implements IBiometricActions {

    private static final String LOG_TAG = "GigyaBiometricV28";

    public GigyaBiometricV28(@Nullable String title, @Nullable String subtitle, @Nullable String description) {
        super(title, subtitle, description);
    }

    @Override
    public void optIn(Context context, IGigyaBiometricCallback callback) {

    }

    @Override
    protected void displayBiometricDialog() {

    }

    @Override
    public void showPrompt(Context context, @NonNull IGigyaBiometricCallback callback) {
        BiometricPrompt prompt = new BiometricPrompt.Builder(context)
                .setTitle(title != null ? title : "")
                .setSubtitle(subtitle != null ? subtitle : "")
                .setDescription(description != null ? description : "")
                .setNegativeButton("Cancel", context.getMainExecutor(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .build();
        prompt.authenticate(new CancellationSignal(), context.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
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
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });
    }
    
    @Override
    public void dismiss() {

    }
}
