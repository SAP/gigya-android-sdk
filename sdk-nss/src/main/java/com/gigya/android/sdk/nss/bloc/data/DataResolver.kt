package com.gigya.android.sdk.nss.bloc.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream


class DataResolver(
        private val context: Context) {

    private fun fetchResource(res: String): ByteArray {
        val id = context.resources.getIdentifier(res, "drawable", context.packageName)
        val bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, id)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val byteArray: ByteArray = stream.toByteArray()
        bitmap.recycle()
        stream.close()
        return byteArray
    }

    fun handleDataRequest(request: String, arguments: MutableMap<String, Any>, result: MethodChannel.Result) {
        when (request) {
            "image_resource" -> {
                val url: String? = arguments["url"] as String?
                if (url == null) {
                    result.error("400", "", "")
                    return
                }
                val byteArray: ByteArray = fetchResource(url)
                result.success(byteArray)
            }
        }
    }
}