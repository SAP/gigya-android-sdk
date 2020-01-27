package com.gigya.android.sdk.nss

import com.gigya.android.sdk.nss.channel.MainMethodChannelHandler
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.lang.reflect.Type

class NssViewModel: NssObject() {

    var mMarkup: String? = null

    private val mMainMethodChannelHandler: MainMethodChannelHandler by lazy {
        MainMethodChannelHandler(onInitFromAssets = {
            mMarkup.guard {
                throw RuntimeException("Unable to fetch markup from assets")
            }
            val type: Type = object : TypeToken<HashMap<String, Any>>() {}.type
            val map: HashMap<String, Any> = Gson().fromJson(mMarkup!!, type)
            map

        })
    }

    fun registerMainChannel(forEngine: FlutterEngine) {
        val mainChannel = MethodChannel(forEngine.dartExecutor.binaryMessenger, GigyaNss.CHANNEL_MAIN)
        mainChannel.setMethodCallHandler(mMainMethodChannelHandler)
    }
}