package com.gigya.android.sdk.nss.utils

import com.gigya.android.sdk.GigyaLogger
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type


class NssJsonSerializer : JsonSerializer<Double> {
    override fun serialize(src: Double?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        GigyaLogger.debug("NssJsonSerializer", "serialize double")
        return if (src == src!!.toLong().toDouble()) JsonPrimitive(src.toLong()) else JsonPrimitive(src)
    }

}