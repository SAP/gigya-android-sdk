package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.nss.GigyaNss

class NssActionFactory {

    companion object {
        const val register = "register"
        const val login = "login"
        const val setAccount = "setAccount"
    }

    fun get(action: String): NssAction<*>? {
        return when (action) {
            register -> {
                GigyaNss.dependenciesContainer.get(NssRegistrationAction::class.java)
            }
            login -> {
                GigyaNss.dependenciesContainer.get(NssLoginAction::class.java)
            }
            setAccount -> {
                GigyaNss.dependenciesContainer.get(NssSetAccountAction::class.java)
            }
            else -> null
        }
    }
}