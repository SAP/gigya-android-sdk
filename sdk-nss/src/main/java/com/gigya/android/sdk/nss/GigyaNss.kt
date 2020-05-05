package com.gigya.android.sdk.nss

import com.gigya.android.sdk.Gigya
import com.gigya.android.sdk.containers.IoCContainer
import com.gigya.android.sdk.nss.channel.ApiMethodChannel
import com.gigya.android.sdk.nss.channel.IgnitionMethodChannel
import com.gigya.android.sdk.nss.channel.LogMethodChannel
import com.gigya.android.sdk.nss.channel.ScreenMethodChannel
import com.gigya.android.sdk.nss.engine.NssEngineLifeCycle
import com.gigya.android.sdk.nss.bloc.action.NssSetAccountAction
import com.gigya.android.sdk.nss.bloc.action.NssActionFactory
import com.gigya.android.sdk.nss.bloc.action.NssLoginAction
import com.gigya.android.sdk.nss.bloc.action.NssRegistrationAction
import com.gigya.android.sdk.nss.bloc.flow.NssFlowManager

object GigyaNss {

    val dependenciesContainer: IoCContainer = Gigya.getContainer()

    // Only ARM based architectures are supported.
    private val SUPPORTED_DEVICE_ARCHITECTURES = arrayListOf("armv7l", "aarch64", "arm64-v8a", "armeabi-v7a")

    /**
     * The native screensets engine supports only "ARM" architectures as a direct result of using the Flutter framework.
     * This method will check and verify that the feature is available for this specific device.
     * Do not use this method for testing on x86 emulator instances.
     */
    fun isSupported(): Boolean {
        System.getProperty("os.arch")?.let { arch ->
            if (SUPPORTED_DEVICE_ARCHITECTURES.contains(arch)) return true
        }
        return false
    }

    /**
     * Register Nss dependencies.
     * This method must be called prior to first use of the library.
     */
    fun register() {
        dependenciesContainer
                .bind(NssEngineLifeCycle::class.java, NssEngineLifeCycle::class.java, false)
                .bind(IgnitionMethodChannel::class.java, IgnitionMethodChannel::class.java, true)
                .bind(ApiMethodChannel::class.java, ApiMethodChannel::class.java, true)
                .bind(ScreenMethodChannel::class.java, ScreenMethodChannel::class.java, true)
                .bind(LogMethodChannel::class.java, LogMethodChannel::class.java, true)
                .bind(NssFlowManager::class.java, NssFlowManager::class.java, false)
                .bind(NssRegistrationAction::class.java, NssRegistrationAction::class.java, false)
                .bind(NssLoginAction::class.java, NssLoginAction::class.java, false)
                .bind(NssSetAccountAction::class.java, NssSetAccountAction::class.java, false)
                .bind(NssActionFactory::class.java, NssActionFactory::class.java, false)
                .bind(NssFlowViewModel::class.java, NssFlowViewModel::class.java, true)
    }

    /**
     * Load markup JSON file from assets folder.
     * @param withAsset Asset JSON file name.
     * @return  Nss.Builder instance. Use builder response to continue to flow.
     */
    fun load(withAsset: String): Nss.Builder {
        return Nss.Builder().assetPath(withAsset)
    }

}