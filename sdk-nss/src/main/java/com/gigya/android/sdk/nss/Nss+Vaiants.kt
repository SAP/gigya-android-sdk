package com.gigya.android.sdk.nss

fun <T> T.guard(block: T.() -> Unit): T {
    if (this == null) block(); return this
}

inline fun <reified T> Any?.refine(block: T.() -> Unit) {
    if (this is T) {
        block()
    }
}