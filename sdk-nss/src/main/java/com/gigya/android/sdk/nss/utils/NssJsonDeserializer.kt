package com.gigya.android.sdk.nss.utils

import com.gigya.android.sdk.GigyaLogger
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.internal.LinkedTreeMap
import java.lang.reflect.Type
import java.util.*
import kotlin.math.ceil

class NssJsonDeserializer : JsonDeserializer<Map<String, Any>> {

    @Suppress("UNCHECKED_CAST")
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Map<String, Any> {
        GigyaLogger.debug("NssJsonDeserializer", "deserialize")
        return read(json!!) as Map<String, Any>
    }

    private fun read(`in`: JsonElement): Any? {
        GigyaLogger.debug("NssJsonDeserializer", "read")
        if (`in`.isJsonArray) {
            val list: MutableList<Any?> = ArrayList()
            val arr = `in`.asJsonArray
            for (anArr in arr) {
                list.add(read(anArr))
            }
            return list
        } else if (`in`.isJsonObject) {
            val map: MutableMap<String, Any?> = LinkedTreeMap()
            val obj = `in`.asJsonObject
            val entitySet = obj.entrySet()
            for ((key, value) in entitySet) {
                map[key] = read(value)
            }
            return map
        } else if (`in`.isJsonPrimitive) {
            val prim = `in`.asJsonPrimitive
            if (prim.isBoolean) {
                return prim.asBoolean
            } else if (prim.isString) {
                return prim.asString
            } else if (prim.isNumber) {
                val num = prim.asNumber
                return if (ceil(num.toDouble()) == num.toLong().toDouble()) num.toLong() else {
                    num.toDouble()
                }
            }
        }
        return null
    }
}