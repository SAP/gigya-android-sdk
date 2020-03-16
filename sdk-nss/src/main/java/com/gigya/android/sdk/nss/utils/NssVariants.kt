package com.gigya.android.sdk.nss.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun <T> T.guard(block: T.() -> Unit): T {
    if (this == null) block(); return this
}

fun <T> T.serializeToMap(gson: Gson): Map<String, Any> {
    return convert(gson)
}

inline fun <reified T> Map<String, Any>.toDataClass(gson: Gson): T {
    return convert(gson)
}

inline fun <I, reified O> I.convert(gson: Gson): O {
    val json = gson.toJson(this)
    return gson.fromJson(json, object : TypeToken<O>() {}.type)
}

inline fun <K, reified V> String.serialize(gson: Gson): Map<K, V> {
    return gson.fromJson(this, object : TypeToken<Map<K, V>>() {}.type)
}

inline fun <reified T> Any?.refine(block: T.() -> Unit) {
    if (this is T) {
        block()
    }
}

inline fun <reified T> Any?.refined(block: (T) -> Unit) {
    if (this is T) {
        block(this)
    }
}

inline fun <reified T> Any?.refine(block: T.() -> Unit, ex: () -> Unit) {
    if (this is T) {
        block()
    } else {
        ex()
    }
}