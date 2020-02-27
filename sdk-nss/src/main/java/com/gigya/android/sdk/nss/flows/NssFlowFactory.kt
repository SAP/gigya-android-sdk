package com.gigya.android.sdk.nss.flows

import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.nss.GigyaNss

class NssFlowFactory<T : GigyaAccount>(private val businessApiService: IBusinessApiService<T>) {

    fun createFor(identifier: String): NssFlow<*>? {
        return when (identifier) {
            Flow.REGISTRATION.identifier -> {
                GigyaNss.dependenciesContainer.get(NssRegistrationFlow::class.java)
            }
            Flow.LOGIN.identifier -> {
                GigyaNss.dependenciesContainer.get(NssLoginFlow::class.java)
            }
            else -> null
        }
    }

    internal enum class Flow(val identifier: String) {
        REGISTRATION("registration"), LOGIN("login")
    }
}