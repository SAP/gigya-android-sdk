package com.gigya.android.sdk.nss.bloc.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import com.gigya.android.sdk.GigyaLogger
import io.flutter.plugin.common.MethodChannel
import java.io.ByteArrayOutputStream


class NssDataResolver(private val context: Context) {

    companion object {
        const val LOG_TAG = "NssDataResolver"
    }

    /**
     * Generate a bitmap from a given resource Id and decode it.
     * Decoded byte array will be passed the the NSS engine and reconstructed.
     */
    private fun getBitmapDataFromResourceId(res: String): ByteArray? {
        val id = context.resources.getIdentifier(res, "drawable", context.packageName)
        val bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, id)
        return getBitmapData(bitmap)
    }

    fun getBitmapDataFromUri(uri: Uri): ByteArray? {
        val bitmap: Bitmap? = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        bitmap?.let {
            return getBitmapData(it)
        }
        return null
    }

    fun getBitmapData(bitmap: Bitmap): ByteArray? {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray: ByteArray = stream.toByteArray()
        bitmap.recycle()
        stream.close()
        return byteArray
    }

    /**
     * Fetch image data from resource according to provided Id.
     * Byte data will be injected back to the engine for display.
     */
    fun fetchImageResource(arguments: MutableMap<String, Any>, result: MethodChannel.Result) {
        val url: String? = arguments["url"] as String?
        if (url == null) {
            // Return empty data. Should direct to fallback.
            result.success(null)
            return
        }
        val byteArray: ByteArray? = getBitmapDataFromResourceId(url)
        result.success(byteArray)
    }

    /**
     * Compose an image selection intent (capture & gallery options).
     */
    @SuppressLint("InlinedApi")
    fun imageSelectionIntent(): Intent {
        val capture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val select = Intent(Intent.ACTION_GET_CONTENT).addCategory(Intent.CATEGORY_OPENABLE)
        select.type = "image/*"
        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, select)
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(capture))
        return chooserIntent
    }
}