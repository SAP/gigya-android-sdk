package com.gigya.android.sdk.nss

import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.network.GigyaError

abstract class NssEvents<T : GigyaAccount> {

    open fun onError(error: GigyaError) {
        // Stub.
    }

    open fun onCancel() {
        // Stub.
    }

    open fun onLogin(accountObj: T) {
        // Stub.
    }
}