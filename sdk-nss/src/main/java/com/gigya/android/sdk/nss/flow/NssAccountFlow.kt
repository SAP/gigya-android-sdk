package com.gigya.android.sdk.nss.flows

import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.serializeToMap
import io.flutter.plugin.common.MethodChannel

class NssAccountFlow<T : GigyaAccount>(override val bApi: IBusinessApiService<T>) : NssFlow<T>(bApi) {

    companion object {
        const val LOG_TAG = "NssAccountFlow"
        const val includeAll = "identities-active , identities-all , identities-global , loginIDs ," +
                " emails, profile, data, password, isLockedOut, lastLoginLocation, regSource, irank, rba, subscriptions, userInfo, preferences"
        const val extraProfileFieldsAll = "languages, address, phones, education, educationLevel," +
                " honors, publications,  patents, certifications, professionalHeadline, bio, industry," +
                " specialties, work, skills, religion, politicalView, interestedIn, relationshipStatus," +
                " hometown, favorites, followersCount, followingCount, username, name, locale, verified, timezone, likes, samlData"
    }

    override fun initialize(result: MethodChannel.Result) {
        bApi.getAccount(mapOf("include" to includeAll, "extraProfileFields" to extraProfileFieldsAll), object : GigyaCallback<T>() {

            override fun onSuccess(obj: T) {
                val map = obj.serializeToMap(gson).guard {
                    throw RuntimeException("Failed to serialize account object")
                }
                result.success(map)
            }

            override fun onError(error: GigyaError?) {
                error?.let {
                    GigyaLogger.error(LOG_TAG, "initialize() failed with: ${it.localizedMessage}")
                    result.error(it.errorCode.toString(), it.localizedMessage, it.data)
                }
            }

        })
    }

    override fun onNext(method: String, arguments: Map<String, Any>?, result: MethodChannel.Result) {
        super.onNext(method, arguments, result)
        if (method == "submit") {

        }
    }

    override fun onDispose() {
        // Stub.
    }

}