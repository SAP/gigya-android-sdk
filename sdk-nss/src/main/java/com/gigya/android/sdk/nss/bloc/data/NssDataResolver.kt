package com.gigya.android.sdk.nss.bloc.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream


class NssDataResolver(
        private val context: Context) {

    /**
     * Generate a bitmap from a given resource Id and decode it.
     * Decoded byte array will be passed the the NSS engine and reconstructed.
     */
    private fun getBitmapDataFromResourceId(res: String): ByteArray {
        val id = context.resources.getIdentifier(res, "drawable", context.packageName)
        val bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, id)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray: ByteArray = stream.toByteArray()
        bitmap.recycle()
        stream.close()
        return byteArray
    }

    /**
     * Handle specific incoming data requests from the NSS engine.
     * Specific UI components are able to send data requests in order to receive specific native data.
     */
    fun handleDataRequest(request: String, arguments: MutableMap<String, Any>, result: MethodChannel.Result) {
        when (request) {
            "image_resource" -> {
                val url: String? = arguments["url"] as String?
                if (url == null) {
                    // Return empty data. Should direct to fallback.
                    result.success(null)
                    return
                }
                val byteArray: ByteArray = getBitmapDataFromResourceId(url)
                result.success(byteArray)
            }
        }
    }
}