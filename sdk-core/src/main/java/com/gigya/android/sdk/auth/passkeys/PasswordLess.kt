package com.gigya.android.sdk.auth.passkeys

import com.gigya.android.sdk.auth.models.WebAuthnKeyModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class PasswordLessKeyType {

    /**
     * Key generated via FIDO2/WebAuthn API. Deprecated in favor of PASSKEY.
     */
    FIDO2,

    /**
     * Passkey generated via Credentials Manager API.
     */
    PASSKEY
}

data class PasswordLessKey(
    val key: String?,
    val type: PasswordLessKeyType)

class PasswordLessKeyUtils {

    companion object {
        const val LOG_TAG = "PasswordLessKeyUtils"
    }

    private val gson = Gson()

    /**
     * Migrate FIDO2 key data structure to new Passwordless key structure to support
     * both CM & FIDO2.
     */
    fun migratePasswordLessMetaData(oldMetaData: String?, newMetaData: String?): String {
        val fidoKeys = WebAuthnKeyModel.parseList(oldMetaData)
        var newMap: MutableMap<String, List<PasswordLessKey>>? = null
        newMap = if (newMetaData != null) {
            deserialize(newMetaData)
        } else {
            mutableMapOf()
        }
        if (fidoKeys.isNotEmpty()) {
            for (fidoKey in fidoKeys) {
                val keyListForUid = newMap[fidoKey.uid]
                val newKeyData = PasswordLessKey(fidoKey.key, PasswordLessKeyType.FIDO2)
                if (keyListForUid == null) {
                    newMap[fidoKey.uid] = listOf(newKeyData)
                } else {
                    newMap[fidoKey.uid] = keyListForUid.plus(newKeyData)
                }
            }
        }
        return gson.toJson(newMap)
    }

    /**
     * Serialization of the saved key map.
     */
    fun serialize(key: String, passwordLessKey: PasswordLessKey, json: String?): String {
        // Parse the existing JSON string into a HashMap or create a new one
        val type = object : TypeToken<MutableMap<String, MutableList<PasswordLessKey>>>() {}.type
        val dataMap: MutableMap<String, MutableList<PasswordLessKey>> =
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
    fun deserialize(json: String): MutableMap<String, List<PasswordLessKey>> {
        // Define the type for the HashMap
        val type = object : TypeToken<MutableMap<String, List<PasswordLessKey>>>() {}.type
        // Parse the JSON string back to a HashMap
        return gson.fromJson(json, type)
    }

    /**
     * Method to check if a passkey exists for a given user ID
     */
    fun hasPasskeyForUser(uid: String, json: String?): Boolean {
        if (uid.isEmpty() || json.isNullOrEmpty()) return false

        // Deserialize the JSON string into a map
        val type = object : TypeToken<Map<String, List<PasswordLessKey>>>() {}.type
        val dataMap: Map<String, List<PasswordLessKey>> = gson.fromJson(json, type)

        // Check if the user ID exists and has associated passkeys
        return dataMap[uid]?.isNotEmpty() == true
    }
}