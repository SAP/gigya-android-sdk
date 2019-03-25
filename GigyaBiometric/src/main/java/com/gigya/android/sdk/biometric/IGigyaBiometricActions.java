package com.gigya.android.sdk.biometric;

import android.content.Context;
import android.support.annotation.NonNull;

public interface IGigyaBiometricActions {

    void showPrompt(Context context, GigyaBiometric.Action action,  @NonNull GigyaPromptInfo gigyaPromptInfo, int encryptionMode,
                    final @NonNull IGigyaBiometricCallback callback);

}
