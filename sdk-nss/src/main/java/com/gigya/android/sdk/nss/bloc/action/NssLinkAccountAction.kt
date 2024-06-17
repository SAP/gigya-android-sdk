package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.interruption.link.LinkAccountsResolver
import com.gigya.android.sdk.nss.bloc.data.NssJsEvaluator
import com.gigya.android.sdk.nss.bloc.flow.NssResolver
import com.gigya.android.sdk.nss.utils.guard
import com.gigya.android.sdk.nss.utils.refined
import com.gigya.android.sdk.nss.utils.serializeToMap
import io.flutter.plugin.common.MethodChannel

class NssLinkAccountAction<T : GigyaAccount>(private val businessApi: IBusinessApiService<T>,
                                             jsEvaluator: NssJsEvaluator)
    : NssAction<T>(businessApi, jsEvaluator) {

    companion object {
        const val LOG_TAG = "NssLinkAccountAction"

        const val CONFLICTING_ACCOUNT_KEY = "conflictingAccount"
    }

    override fun initialize(expressions: Map<String, String>, result: MethodChannel.Result) {
        // Inject conflicting account data back to engine.
        flowDelegate?.getResolver()?.let { nssResolver ->
            nssResolver.refined<NssResolver<LinkAccountsResolver<T>>> { linkAccountResolver ->
                val data = mapOf<String, Any>(CONFLICTING_ACCOUNT_KEY to linkAccountResolver.resolver.conflictingAccounts.serializeToMap(flowDelegate!!.getGson()))
                doExpressions(data, expressions, result)
            }
        }
    }

    override fun onNext(method: String, arguments: MutableMap<String, Any>?) {
        flowDelegate?.guard {
            GigyaLogger.error(NssSetAccountAction.LOG_TAG, "Action flow delegate not set")
        }

        flowDelegate!!.getResolver()?.refined<NssResolver<LinkAccountsResolver<T>>> { resolver ->
            when (method) {
                socialLogin -> {
                    // Link to social.
                    val provider = arguments?.get("provider") as? String
                    provider?.guard {
                        GigyaLogger.error(NssAction.LOG_TAG, "Social provider unavailable")
                    }

                    resolver.resolver.linkToSocial(provider)
                }
                submit -> {
                    // Link to site.
                    val loginID = arguments?.get("loginID") as? String
                    loginID?.guard {
                        GigyaLogger.error(NssAction.LOG_TAG, "Missing required loginID parameter")
                    }

                    val pass = arguments?.get("password") as? String
                    pass?.guard {
                        GigyaLogger.error(NssAction.LOG_TAG, "Missing required password parameter")
                    }

                    resolver.resolver.linkToSite(loginID, pass)
                }
            }
        }

    }

}