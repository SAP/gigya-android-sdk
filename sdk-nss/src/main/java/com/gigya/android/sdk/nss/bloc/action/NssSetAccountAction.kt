package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.GigyaCallback
import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.interruption.IPendingRegistrationResolver
import com.gigya.android.sdk.interruption.PendingRegistrationResolver
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.Nss
import com.gigya.android.sdk.nss.bloc.flow.INssFlowDelegate
import com.gigya.android.sdk.nss.bloc.flow.NssResolver
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import com.gigya.android.sdk.nss.utils.serializeToMap
import io.flutter.plugin.common.MethodChannel

class NssSetAccountAction<T : GigyaAccount>(private val businessApi: IBusinessApiService<T>) : NssAction<T>() {

    companion object {
        const val LOG_TAG = "NssAccountFlow"

        const val includeAll = "identities-active , identities-all , identities-global , loginIDs ," +
                " emails, profile, data, password, isLockedOut, lastLoginLocation, regSource, irank, rba, subscriptions, userInfo, preferences"
        const val extraProfileFieldsAll = "languages, address, phones, education, educationLevel," +
                " honors, publications,  patents, certifications, professionalHeadline, bio, industry," +
                " specialties, work, skills, religion, politicalView, interestedIn, relationshipStatus," +
                " hometown, favorites, followersCount, followingCount, username, name, locale, verified, timezone, likes, samlData"
    }

    /**
     * Action may use a resolver as part of a registration/login flow.
     */
    var pendingRegistrationResolver: IPendingRegistrationResolver? = null

    override fun initialize(result: MethodChannel.Result) {
        val params = mutableMapOf<String, Any>(
                "include" to includeAll,
                "extraProfileFields" to extraProfileFieldsAll
        )
        flowDelegate?.getResolver()?.let { nssResolver ->
            nssResolver.refined<NssResolver<IPendingRegistrationResolver>> {
                pendingRegistrationResolver = it.resolver
                params["regToken"] = pendingRegistrationResolver!!.regToken
            }
        }

        businessApi.getAccount(params, object : GigyaCallback<T>() {

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

    override fun onNext(method: String, arguments: MutableMap<String, Any>?) {
        flowDelegate.guard {
            GigyaLogger.error(LOG_TAG, "Action flow delegate not set")
        }

        super.onNext(method, arguments)
        if (method == submit) {

            flattenArguments(arguments)

            if (pendingRegistrationResolver != null) {
                // Resolve interruption as part of a registration/login flow.
                pendingRegistrationResolver?.setAccount(arguments!!)

            } else {
                flowDelegate!!.refined<INssFlowDelegate<T>> {
                    // Apply default set account action using the business api.
                    businessApi.setAccount(arguments, it.getMainFlowCallback())
                }
            }
        }
    }

}