package com.gigya.android.sample

import com.gigya.android.sample.ui.BiometricPushTFAActivity
import com.gigya.android.sdk.tfa.push.firebase.GigyaFirebaseMessagingService

class GigyaFirebaseMessagingExt : GigyaFirebaseMessagingService() {

    override fun getCustomActionActivity(): Class<*> {
        return BiometricPushTFAActivity::class.java
    }
}