package com.gigya.android.sdk.biometric;

public interface IGigyaBiometricCallback {

    void onBiometricOperationSuccess();

    void onBiometricOperationFailed(String reason);

    void onBiometricOperationCanceled();
}
