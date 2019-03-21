package com.gigya.android.sdk.biometric;

import android.content.Context;
import android.support.annotation.NonNull;

public interface IBiometricActions {

    void showPrompt(Context context, @NonNull IGigyaBiometricCallback callback);

    void dismiss();

}
