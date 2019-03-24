package com.gigya.android.sdk.biometric;

import android.content.Context;
import android.support.annotation.NonNull;

import com.gigya.android.sdk.biometric.model.GigyaPromptInfo;

public interface IGigyaBiometricActions {

    void showPrompt(Context context, GigyaBiometric.Action action,  @NonNull GigyaPromptInfo gigyaPromptInfo, int encryptionMode,
                    final @NonNull IGigyaBiometricCallback callback, final @NonNull Runnable onAuthenticated);

}
