package com.gigya.android.sdk.biometric;

import android.support.annotation.NonNull;

public interface IGigyaBiometricOperationCallback {

    void onBiometricOperationSuccess(@NonNull GigyaBiometric.Action action);

    void onBiometricOperationFailed(String reason);
}
