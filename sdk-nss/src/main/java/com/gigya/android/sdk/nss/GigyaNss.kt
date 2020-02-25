package com.gigya.android.sdk.nss

import android.annotation.SuppressLint
import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.containers.IoCContainer
import com.gigya.android.sdk.nss.channel.ApiMethodChannel
import com.gigya.android.sdk.nss.channel.MainMethodChannel
import com.gigya.android.sdk.nss.flows.NssFlowFactory
import com.gigya.android.sdk.nss.flows.NssLoginFlow
import com.gigya.android.sdk.nss.flows.NssRegistrationFlow

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
        dependenciesContainer.bind(MainMethodChannel::class.java, MainMethodChannel::class.java, false)
                .bind(ApiMethodChannel::class.java, ApiMethodChannel::class.java, false)
                .bind(NssRegistrationFlow::class.java, NssRegistrationFlow::class.java, false)
                .bind(NssLoginFlow::class.java, NssLoginFlow::class.java, false)
                .bind(NssFlowFactory::class.java, NssFlowFactory::class.java, false)
                .bind(NssViewModel::class.java, NssViewModel::class.java, true)
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