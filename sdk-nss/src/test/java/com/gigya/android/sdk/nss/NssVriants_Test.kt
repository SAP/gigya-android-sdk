package com.gigya.android.sdk.nss

import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.nss.utils.NssJsonDeserializer
import com.gigya.android.sdk.nss.utils.serializeToMap
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import java.lang.reflect.Type


class NssVriants_Test {

    @Test
    fun testSerializing() {

        val gson = GsonBuilder().registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, NssJsonDeserializer()).create()

        val obj = GigyaAccount()
        obj.createdTimestamp = System.currentTimeMillis()
        obj.apiVersion = 3

        val serialized = obj.serializeToMap(gson)

        val json = "{ \"a\" : 2.2 , \"b\" : 3}"

        val mapType = object : TypeToken<Map<String, Any>>() {}.type
        var tutorialMap: Map<String, Any> = gson.fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)


        assertNotNull(serialized)
    }

    class DoubleSerializer : JsonSerializer<Double?> {

        override fun serialize(src: Double?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return if (src == src!!.toLong().toDouble()) JsonPrimitive(src.toLong()) else JsonPrimitive(src)
        }

    }
}

