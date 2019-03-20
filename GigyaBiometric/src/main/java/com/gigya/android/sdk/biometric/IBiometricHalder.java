package com.gigya.android.sdk.biometric;

import android.content.Context;

public interface IBiometricHalder {

    void showPrompt(Context context);

    void dismiss();

}
