package com.gigya.android.sdk.biometric;

import android.content.Context;

import com.gigya.android.sdk.Gigya;
import com.gigya.android.sdk.biometric.utils.BiomerticUtils;
import com.gigya.android.sdk.biometric.v28.GigyaBiometricV28;


public class GigyaBiometric {

    private Gigya _gigya;

    public GigyaBiometric(Context context) {
        _gigya = Gigya.getInstance();
    }

    public void optIn(Context context) {
        if (BiomerticUtils.isPromptEnabled()) {
            new GigyaBiometricV28().showPrompt(context);
        }
    }

    private void displayBiometricDialog() {

    }
}
