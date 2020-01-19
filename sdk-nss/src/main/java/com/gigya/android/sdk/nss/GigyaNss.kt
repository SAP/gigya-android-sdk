package com.gigya.android.sdk.nss

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import com.gigya.android.sdk.Gigya

object GigyaNss {

    private val mAppContext: Context by lazy<Context> {
      Gigya.getContainer().get(Application::class.java)
    }

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
     * Check supported architectures.
     * The native screensets engine supports only "ARM" architectures as a direct result of using the Flutter framework.
     */
    @SuppressLint("DefaultLocale")
    private fun isSupportedDeviceArchitecture(): Boolean {
        System.getProperty("os.arch")?.let { arch ->
            if (SUPPORTED_DEVICE_ARCHITECTURES.contains(arch)) return true
        }
        return false
    }

    /**
     * ...
     * @return Whether this call is supported. If device architecture does not support the use of the native screensets feature,
     * this function will return FALSE. Consider using the core SDK screensets feature in this case to provide a fallback implementation.
     */
    fun showScreenSet(launcher: Activity): Boolean = when (isSupportedDeviceArchitecture()) {
        true -> {
            NativeScreenSetsActivity.launch(launcher)
            true
        }
        else -> false
    }

    private var mScreenSetsBuilder: ScreenSetsBuilder? = null

    fun loadFromAssets(withPath: String): ScreenSetsBuilder {
        if (mScreenSetsBuilder == null) {
            mScreenSetsBuilder = ScreenSetsBuilder(mAppContext)
        }
        mScreenSetsBuilder!!.assetPath = withPath
        return mScreenSetsBuilder!!
    }

    fun show(launcherContext: Context, screenId: String) {

    }

}