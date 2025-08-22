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
        val type = object : TypeToken<MutableMap<String, MutableList<WebAuthnKeyModel>>>() {}.type
        val dataMap: MutableMap<String, MutableList<WebAuthnKeyModel>> =
            if (json.isNullOrEmpty()) mutableMapOf() else gson.fromJson(json, type)

        // Add the PasswordLessKey object to the list for the given key
        val list = dataMap.getOrPut(key) { mutableListOf() }
        list.add(passwordLessKey)

        // Convert the updated HashMap to JSON
        return gson.toJson(dataMap)
    }

    /**
     * Deserialization of the saved key map.
     */
    fun deserialize(json: String): MutableMap<String, List<WebAuthnKeyModel>> {
        // Define the type for the HashMap
        val type = object : TypeToken<MutableMap<String, List<WebAuthnKeyModel>>>() {}.type
        // Parse the JSON string back to a HashMap
        return gson.fromJson(json, type)
    }

    /**
     * Method to check if a passkey exists for a given user ID
     */
    fun hasPasskeyForUser(uid: String, json: String?): Boolean {
        if (uid.isEmpty() || json.isNullOrEmpty()) return false

        // Deserialize the JSON string into a map
        val type = object : TypeToken<Map<String, List<WebAuthnKeyModel>>>() {}.type
        val dataMap: Map<String, List<WebAuthnKeyModel>> = gson.fromJson(json, type)

        // Check if the user ID exists and has associated passkeys
        return dataMap[uid]?.isNotEmpty() == true
    }
}