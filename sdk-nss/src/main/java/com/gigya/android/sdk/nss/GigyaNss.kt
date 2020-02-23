package com.gigya.android.sdk.nss

import android.annotation.SuppressLint

object GigyaNss {

    const val FLUTTER_ENGINE_ID = "nss_engine_id"

    // Main communication method channel with the Flutter engine. (initialization etc)
    const val CHANNEL_MAIN = "gigya_nss_engine/method/main"

    // API communication method channel with the Flutter engine.
    const val CHANNEL_API = "gigya_nss_engine/method/api"


    // Only ARM based architectures are supported.
    //TODO Make sure all relevant architecture are added.
    private val SUPPORTED_DEVICE_ARCHITECTURES = arrayListOf("armv7l", "aarch64", "arm64-v8a", "armeabi-v7a")

    /**
     * The native screensets engine supports only "ARM" architectures as a direct result of using the Flutter framework.
     * This method will check and verify that the feature is available for this specific device.
     */
    @SuppressLint("DefaultLocale")
    fun isSupportedDeviceArchitecture(): Boolean {
        System.getProperty("os.arch")?.let { arch ->
            if (SUPPORTED_DEVICE_ARCHITECTURES.contains(arch)) return true
        }
        return false
    }

    //region Host interface

    /**
     * Load markup JSON file from assets folder.
     * @param withName Asset JSON file name.
     * @return NssBuilder instance. Use builder response to continue to flow.
     */
    fun loadFromAssets(withName: String): NssBuilder {
        val builder = NssBuilder()
        builder.clear()
        builder.assetPath = withName
        return builder
    }

    //endregion

}