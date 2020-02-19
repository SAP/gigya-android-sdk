package com.gigya.android.sdk.nss.coordinator

import com.gigya.android.sdk.account.models.GigyaAccount

class NssCoordinatorFactory {

    companion object {

        fun <T : GigyaAccount> createFor(identifier: String, whenComplete: (withAction: String) -> Unit): NssCoordinator<T>? {
            return when (identifier) {
                "accounts.register" -> {
                    NssRegistrationCoordinator(whenComplete)
                }
                else -> null
            }
        }
    }

}