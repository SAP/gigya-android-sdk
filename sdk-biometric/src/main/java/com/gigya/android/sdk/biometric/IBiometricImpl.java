package com.gigya.android.sdk.biometric;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public interface IBiometricImpl {

    void showPrompt(final FragmentActivity activity,
                    GigyaBiometric.Action action,
                    @NonNull GigyaPromptInfo gigyaPromptInfo,
                    int encryptionMode,
                    final @NonNull IGigyaBiometricCallback callback);

}
