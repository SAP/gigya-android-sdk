package com.gigya.android.sdk.nss.coordinator

import com.gigya.android.sdk.account.models.GigyaAccount
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

abstract class NssCoordinator<T : GigyaAccount>(open val whenComplete: (withAction: String) -> Unit) : NasCoordinatorLifecycle {

    val gson = Gson()

    /**
     * Data class to map.
     */
    fun <T> T.serializeToMap(): Map<String, Any> {
        return convert()
    }

    /**
     * Map to data class.
     */
    inline fun <reified T> Map<String, Any>.toDataClass(): T {
        return convert()
    }

    /**
     * Convert object of type I to object of type O.
     */
    inline fun <I, reified O> I.convert(): O {
        val json = gson.toJson(this)
        return gson.fromJson(json, object : TypeToken<O>() {}.type)
    }
}