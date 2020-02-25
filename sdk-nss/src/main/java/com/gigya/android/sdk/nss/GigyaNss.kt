package com.gigya.android.sdk.nss

import android.annotation.SuppressLint
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.containers.IoCContainer

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

    val dependenciesContainer: IoCContainer = Gigya.getContainer()

    fun register() {
        dependenciesContainer.bind(NssViewModel::class.java, NssViewModel::class.java, false)
    }

    //region Host interface

    /**
     * Load markup JSON file from assets folder.
     * @param withAsset Asset JSON file name.
     * @return  Nss.Builder instance. Use builder response to continue to flow.
     */
    fun load(withAsset: String): Nss.Builder {
        return Nss.Builder().assetPath(withAsset)
    }

    //endregion

}