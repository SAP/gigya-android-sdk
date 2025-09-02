package com.gigya.android.sdk.auth.passkeys

import com.gigya.android.sdk.auth.models.WebAuthnKeyModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.collections.isNotEmpty

class PasswordLessKeyUtils {

    companion object {
        const val LOG_TAG = "PasswordLessKeyUtils"
    }

    private val gson = Gson()


    /**
     * Serialization of the saved key map.
     */
    fun serialize(key: String, passwordLessKey: WebAuthnKeyModel, json: String?): String {
        // Parse the existing JSON string into a HashMap or create a new one
        val type = object : TypeToken<MutableMap<String, WebAuthnKeyModel>>() {}.type
        val dataMap: MutableMap<String, WebAuthnKeyModel> =
            if (json.isNullOrEmpty()) mutableMapOf() else gson.fromJson(json, type)

        // Add the PasswordLessKey object to the list for the given key
        dataMap[key] = passwordLessKey

        // Convert the updated HashMap to JSON
        return gson.toJson(dataMap)
    }

    /**
     * Deserialization of the saved key map.
     */
    fun deserialize(json: String): MutableMap<String, WebAuthnKeyModel> {
        // Define the type for the HashMap
        val type = object : TypeToken<MutableMap<String, WebAuthnKeyModel>>() {}.type
        // Parse the JSON string back to a HashMap
        return gson.fromJson(json, type)
    }

    /**
     * Method to check if a passkey exists for a given user ID
     */
    fun hasPasskey(json: String?): Boolean {
        if (json.isNullOrEmpty()) return false

        // Deserialize the JSON string into a map
        val dataMap: Map<String, WebAuthnKeyModel> = deserialize(json)

        // Check if the user ID exists and has associated passkeys
        return dataMap.isNotEmpty()
    }

    fun remove(json: String, key: String): String {
        val dataMap = deserialize(json)
        dataMap.remove(key)
        return gson.toJson(dataMap)
    }

    fun getKeyFromStoredPassKey(id: String, json: String): String? {
        val dataMap: Map<String, WebAuthnKeyModel> = deserialize(json)
        return if (dataMap.containsKey(id)) {
            dataMap[id]?.key
        } else {
            null
        }
    }
}