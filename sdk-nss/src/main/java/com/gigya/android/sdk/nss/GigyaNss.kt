package com.gigya.android.sdk.nss

import android.annotation.SuppressLint
import android.app.Activity

object GigyaNss {

    private const val SUPPORTED_DEVICE_ARCHITECTURE = "ARM"

    // Communication channels.
    const val CHANNEL_PLATFORM = "gigya_nss_engine/method/platform"
    const val CHANNEL_LOGIC = "gigya_nss_engine/method/logic"
    const val CHANNEL_SDK = "gigya_nss_engine/method/sdk"
    const val CHANNEL_EVENT = "gigya_nss_engine/event/set"

    /**
     * Check supported architectures.
     * The native screensets engine supports only "ARM" architectures as a direct result of using the Flutter framework.
     */
    @SuppressLint("DefaultLocale")
    fun isSupportedDeviceArchitecture(): Boolean = true
//            when (System.getProperty("os.arch")?.substring(0, 3)?.toUpperCase()) {
//                SUPPORTED_DEVICE_ARCHITECTURE -> true
//                else -> false
//            }

    /**
     * ...
     * @return Whether this call is supported. If device architecture does not support the use of the native screensets feature,
     * this function will return FALSE. Consider using the core SDK screensets feature in this case to provide a fallback implementation.
     */
    fun showScreenSet(launcher: Activity): Boolean =
            when (isSupportedDeviceArchitecture()) {
                true -> {
                    NativeScreenSetsActivity.launch(launcher)
                    true
                }
                else -> false
            }
}