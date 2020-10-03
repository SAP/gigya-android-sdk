package com.gigya.android.sdk.biometric;

import androidx.annotation.NonNull;

public interface IGigyaBiometricOperationCallback {

    void onBiometricOperationSuccess(@NonNull GigyaBiometric.Action action);

    void onBiometricOperationFailed(String reason);
}
