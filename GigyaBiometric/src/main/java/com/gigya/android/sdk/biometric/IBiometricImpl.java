package com.gigya.android.sdk.biometric;

import android.support.annotation.NonNull;

public interface IBiometricImpl {

    void showPrompt(GigyaBiometric.Action action,
                    @NonNull GigyaPromptInfo gigyaPromptInfo,
                    int encryptionMode,
                    final @NonNull IGigyaBiometricCallback callback);

}
