package com.gigya.android.sample.extras

import android.graphics.Bitmap
import android.text.TextUtils
import android.widget.ImageView
import com.bumptech.glide.request.RequestOptions
import com.gigya.android.sample.GlideApp

fun ImageView.loadBitmap(bitmap: Bitmap?) {
    if (bitmap != null) {
        GlideApp.with(this).load(bitmap).into(this)
    }
}

fun ImageView.loadRoundImageWith(url: String?, error: Int) {
    if (!TextUtils.isEmpty(url)) {
        GlideApp.with(this).load(url)
                .error(error)
                .apply(RequestOptions.circleCropTransform()).into(this)
    }
}