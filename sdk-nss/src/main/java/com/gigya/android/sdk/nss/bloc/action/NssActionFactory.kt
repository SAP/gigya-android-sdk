package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.Gigya

class NssActionFactory {

    companion object {
        const val register = "register"
        const val login = "login"
        const val setAccount = "setAccount"
        const val forgotPassword = "forgotPassword"
        const val linkAccount = "linkAccount"
        const val otp = "otp"
    }

    fun get(actionId: String): NssAction<*>? {
        val action = when (actionId) {
            register -> {
                Gigya.getContainer().get(NssRegistrationAction::class.java)
            }
            login -> {
                Gigya.getContainer().get(NssLoginAction::class.java)
            }
            setAccount -> {
                Gigya.getContainer().get(NssSetAccountAction::class.java)
            }
            forgotPassword -> {
                Gigya.getContainer().get(NssForgotPasswordAction::class.java)
            }
            linkAccount -> {
                Gigya.getContainer().get(NssLinkAccountAction::class.java)
            }
            otp -> {
                Gigya.getContainer().get(NssOtpAction::class.java)
            }
            else -> null
        }
        action!!.actionId = actionId
        return action
    }
}