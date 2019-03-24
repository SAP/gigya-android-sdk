package com.gigya.android.sdk.biometric.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

public class GigyaBiometricUtils {

    /**
     * Check if fingerprint permission is available.
     */
    private static boolean isPermissionGranted(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Check for device hardware support.
     * If your application includes android.hardware.fingerprint required="true" this check is not needed.
     */
    @SuppressLint("MissingPermission")
    public static boolean isSupported(Context context) {
        if (!isPermissionGranted(context)) {
            return false;
        }
        FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(context);
        return fingerprintManager.isHardwareDetected();
    }

    /**
     * Check if user has enrolled fingerprints on his device.
     */
    @SuppressLint("MissingPermission")
    public static boolean hasEnrolledFingerprints(Context context) {
        if (!isPermissionGranted(context)) {
            return false;
        }
        FingerprintManagerCompat fingerprintManager = FingerprintManagerCompat.from(context);
        return fingerprintManager.hasEnrolledFingerprints();
    }

    /**
     * Check for BiometricPrompt API availability (Android Pie and above).
     */
    public static boolean isPromptEnabled() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P);
    }

}
