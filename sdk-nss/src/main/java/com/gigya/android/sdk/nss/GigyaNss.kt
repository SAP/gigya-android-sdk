package com.gigya.android.sdk.nss

import android.annotation.SuppressLint

object GigyaNss {

    private val mBuilder: NssBuilder by lazy {
        NssBuilder()
    }

    const val FLUTTER_ENGINE_ID = "nss_engine_id"

    /**
     * Main communication method channel with the Flutter engine (initialization etc.)
     */
    const val CHANNEL_PLATFORM = "gigya_nss_engine/method/platform"

    /**
     * SDK actions method channel.
     */
    const val CHANNEL_SDK = "gigya_nss_engine/method/sdk"

    /**
     * Optional custom logic method channel. Used to apply custom logic on screenset events.
     */
    const val CHANNEL_LOGIC = "gigya_nss_engine/method/logic"

    /**
     * Screensets stream event channel.
     */
    const val CHANNEL_EVENT = "gigya_nss_engine/event/set"

    /*
    Only ARM based architectures are supported.
     */
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

    fun loadFromAssets(withPath: String): NssBuilder {
        mBuilder.assetPath = withPath
        return mBuilder
    }

    //endregion

}