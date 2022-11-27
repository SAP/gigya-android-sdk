package com.gigya.android.sdk.nss.bloc

import com.gigya.android.sdk.GigyaLoginCallback
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.GigyaApiResponse

/**
 * Extension of the general GigyaLoginCallback class used for NSS operations.
 */
abstract class GigyaNssCallback<T : GigyaAccount, R : GigyaApiResponse> : GigyaLoginCallback<T>() {

    abstract fun onGenericResponse(res: R?, api: String?)
}