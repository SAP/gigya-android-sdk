package com.gigya.android.sdk.nss.flow

import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.nss.utils.NssJsonDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

abstract class NssFlow<T : GigyaAccount>(open val bApi: IBusinessApiService<T>? = null) : NssFlowLifecycle {

    open var gson: Gson = GsonBuilder().registerTypeAdapter(object : TypeToken<Map<String?, Any?>?>() {}.type, NssJsonDeserializer()).create()
}
