package com.gigya.android.sdk.biometric;

import android.content.Context;
import android.support.annotation.NonNull;

public interface IGigyaBiometricActions {

    void showPrompt(Context context, final @NonNull IGigyaBiometricCallback callback, final @NonNull Runnable onAuthenticated);

}
