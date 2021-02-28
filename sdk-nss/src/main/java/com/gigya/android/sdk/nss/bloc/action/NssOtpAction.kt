package com.gigya.android.sdk.nss.bloc.action

import com.gigya.android.sdk.GigyaLogger
import com.gigya.android.sdk.account.models.GigyaAccount
import com.gigya.android.sdk.api.GigyaApiResponse
import com.gigya.android.sdk.api.IBusinessApiService
import com.gigya.android.sdk.auth.GigyaAuth
import com.gigya.android.sdk.auth.GigyaOTPCallback
import com.gigya.android.sdk.auth.resolvers.IGigyaOtpResult
import com.gigya.android.sdk.interruption.IPendingRegistrationResolver
import com.gigya.android.sdk.interruption.link.ILinkAccountsResolver
import com.gigya.android.sdk.interruption.tfa.TFAResolverFactory
import com.gigya.android.sdk.interruption.tfa.models.TFAProviderModel
import com.gigya.android.sdk.network.GigyaError
import com.gigya.android.sdk.nss.bloc.GigyaNssCallback
import com.gigya.android.sdk.nss.bloc.data.NssJsEvaluator
import com.gigya.android.sdk.nss.bloc.flow.INssFlowDelegate
import com.gigya.android.sdk.nss.utils.refined
import io.flutter.plugin.common.MethodChannel

enum class State {
    PHONE, CODE
}

class NssOtpAction<T : GigyaAccount>(businessApi: IBusinessApiService<T>,
                                     jsEvaluator: NssJsEvaluator)
    : NssAction<T>(businessApi, jsEvaluator) {

    var state: State = State.PHONE

    private var otpResolver: IGigyaOtpResult? = null

    override fun initialize(expressions: Map<String, String>, result: MethodChannel.Result) {
        GigyaLogger.debug(NssLoginAction.LOG_TAG, "Explicit flow initialization ")
        jsEvaluator.eval(null, expressions) { jsResult ->
            result.success(mapOf("data" to mapOf(), "expressions" to jsEvaluator.mapExpressions(jsResult)))
        }
    }

    override fun onNext(method: String, arguments: MutableMap<String, Any>?) {
        if (arguments == null) {
            return
        }
        flowDelegate!!.refined<INssFlowDelegate<T>> {
            flowDelegate!!.getMainFlowCallback().refined<GigyaNssCallback<T, GigyaApiResponse>> { nssCallback ->
                when (method) {
                    "submit" -> {
                        when (state) {
                            State.PHONE -> {
                                val loginId: String = arguments["phone"] as? String ?: ""
                                val params: Map<String, Any> = arguments["params"] as Map<String, Any>
                                GigyaAuth.getInstance().otp.phoneLogin(loginId, params, object : GigyaOTPCallback<T>() {

                                    override fun onSuccess(obj: T) {
                                        nssCallback.onSuccess(obj)
                                    }

                                    override fun onError(error: GigyaError?) {
                                        nssCallback.onError(error)
                                    }

                                    override fun onPendingOTPVerification(response: GigyaApiResponse, resolver: IGigyaOtpResult) {
                                        otpResolver = resolver
                                        state = State.CODE
                                    }

                                    override fun onPendingRegistration(response: GigyaApiResponse, resolver: IPendingRegistrationResolver) {
                                        nssCallback.onPendingRegistration(response, resolver)
                                    }

                                    override fun onPendingVerification(response: GigyaApiResponse, regToken: String?) {
                                        nssCallback.onPendingVerification(response, regToken)
                                    }

                                    override fun onPendingPasswordChange(response: GigyaApiResponse) {
                                        nssCallback.onPendingPasswordChange(response)
                                    }

                                    override fun onConflictingAccounts(response: GigyaApiResponse, resolver: ILinkAccountsResolver) {
                                        nssCallback.onConflictingAccounts(response, resolver)
                                    }

                                    override fun onIntermediateLoad() {
                                        nssCallback.onIntermediateLoad()
                                    }

                                    override fun onOperationCanceled() {
                                        nssCallback.onOperationCanceled()
                                    }

                                    override fun onPendingTwoFactorRegistration(response: GigyaApiResponse, inactiveProviders: MutableList<TFAProviderModel>, resolverFactory: TFAResolverFactory) {
                                        nssCallback.onPendingTwoFactorRegistration(response, inactiveProviders, resolverFactory)
                                    }

                                    override fun onPendingTwoFactorVerification(response: GigyaApiResponse, activeProviders: MutableList<TFAProviderModel>, resolverFactory: TFAResolverFactory) {
                                        nssCallback.onPendingTwoFactorVerification(response, activeProviders, resolverFactory)
                                    }

                                })
                            }
                            State.CODE -> {
                                val code: String = arguments["code"] as? String ?: ""
                                otpResolver?.verify(code)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun dispose() {
        otpResolver = null
        super.dispose()
    }
}


