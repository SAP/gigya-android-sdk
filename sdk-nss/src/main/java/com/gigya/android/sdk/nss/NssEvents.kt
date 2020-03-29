package com.gigya.android.sdk.nss

import com.gigya.android.sdk.account.models.GigyaAccount

abstract class NssEvents<T : GigyaAccount> {

    open fun onException(cause: String) {
        // Stub.
    }

    open fun onCancel() {
        // Stub.
    }

    open fun onLogin(accountObj: T) {
        // Stub.
    }
}