package com.gigya.android.sdk.nss.flows

import com.gigya.android.sdk.account.models.GigyaAccount

class NssFlowFactory {

    companion object {

        fun <T : GigyaAccount> createFor(identifier: String): NssFlow<T>? {
            return when (identifier) {
                Flow.REGISTRATION.identifier -> {
                    NssRegistrationFlow(identifier)
                }
                else -> null
            }
        }
    }


    internal enum class Flow(val identifier: String) {
        REGISTRATION("registration")
    }
}