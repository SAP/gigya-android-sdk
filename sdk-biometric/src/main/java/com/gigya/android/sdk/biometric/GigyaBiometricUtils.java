package com.gigya.android.sdk.biometric;

import android.content.Context;

import androidx.biometric.BiometricManager;

/**
 * Biometric utility & evaluation class.
 */
class GigyaBiometricUtils {

    static boolean isAvailable(Context context) {
        return canAuthenticate(context) == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /**
     * Returns the raw BiometricManager status code so callers can surface actionable error messages.
     * Possible values: BIOMETRIC_SUCCESS, BIOMETRIC_ERROR_NO_HARDWARE,
     * BIOMETRIC_ERROR_HW_UNAVAILABLE, BIOMETRIC_ERROR_NONE_ENROLLED,
     * BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED.
     */
    static int canAuthenticate(Context context) {
        return BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG);
    }
}
