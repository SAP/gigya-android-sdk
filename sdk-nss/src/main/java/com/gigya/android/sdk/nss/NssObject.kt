package com.gigya.android.sdk.nss

open class NssObject {

    inline fun <T> T.guard(block: T.() -> Unit): T {
        if (this == null) block(); return this
    }

}