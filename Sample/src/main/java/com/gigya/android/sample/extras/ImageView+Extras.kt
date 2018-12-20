package com.gigya.android.sample.extras

import android.text.TextUtils
import android.widget.ImageView
import com.bumptech.glide.request.RequestOptions
import com.gigya.android.sample.GlideApp

fun ImageView.loadWith(url: String?, error: Int) {
    if (!TextUtils.isEmpty(url)) {
        GlideApp.with(this).load(url).error(error).into(this)
    }
}

fun ImageView.loadWith(url: String?) {
    if (!TextUtils.isEmpty(url)) {
        GlideApp.with(this).load(url).into(this)
    }
}

fun ImageView.loadRoundImageWith(url: String?, error: Int) {
    if (!TextUtils.isEmpty(url)) {
        GlideApp.with(this).load(url)
            .error(error)
            .apply(RequestOptions.circleCropTransform()).into(this)
    }

}