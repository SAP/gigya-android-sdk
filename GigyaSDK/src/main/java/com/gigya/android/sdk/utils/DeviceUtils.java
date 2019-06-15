package com.gigya.android.sdk.utils;

import android.os.Build;
import android.support.annotation.NonNull;

public class DeviceUtils {

    /**
     * Get device model name.
     *
     * @return The device model name.
     */
    @NonNull
    public static String getModelName() {
        return Build.MODEL;
    }

    /**
     * Get device manufacturer.
     *
     * @return The device manufacturer.
     */
    @NonNull
    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * Get the system version.
     *
     * @return The system OS version.
     */
    @NonNull
    public static String getOsVersion() {
        return Build.VERSION.RELEASE;
    }
}
