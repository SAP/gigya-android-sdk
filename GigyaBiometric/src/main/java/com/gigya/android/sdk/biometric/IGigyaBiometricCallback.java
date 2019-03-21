package com.gigya.android.sdk.biometric;

import android.annotation.TargetApi;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

public abstract class IGigyaBiometricCallback {

    public abstract void onAuthenticationFailed();

    public abstract void onAuthenticationError(int errMsgId, CharSequence errString);

    public abstract void onAuthenticationHelp(int helpCode, CharSequence helpString);

    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
        // Stub.
    }

    @TargetApi(Build.VERSION_CODES.P)
    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
        // Stub.
    }
}
