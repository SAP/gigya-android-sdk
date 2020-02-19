package com.gigya.android.sdk.nss

import com.gigya.android.sdk.account.models.GigyaAccount

class NssViewModelProviders {

    companion object {

        /**
         * Provide a new instance of the NssActivityViewModel
         */
        fun <T : GigyaAccount> provideViewModel(markup: String) = NssViewModel<T>(markup)

    }
}