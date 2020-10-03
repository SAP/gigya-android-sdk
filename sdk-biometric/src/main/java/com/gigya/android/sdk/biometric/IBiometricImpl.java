package com.gigya.android.sdk.biometric;

import android.app.Activity;
import androidx.annotation.NonNull;

public interface IBiometricImpl {

    void showPrompt(final Activity activity,
                    GigyaBiometric.Action action,
                    @NonNull GigyaPromptInfo gigyaPromptInfo,
                    int encryptionMode,
                    final @NonNull IGigyaBiometricCallback callback);

}
