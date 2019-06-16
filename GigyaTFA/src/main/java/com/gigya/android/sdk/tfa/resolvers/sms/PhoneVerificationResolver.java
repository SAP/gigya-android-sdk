package com.gigya.android.sdk.tfa.resolvers.sms;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.Resolver;
import com.gigya.android.sdk.interruption.tfa.models.TFACompleteVerificationModel;
import com.gigya.android.sdk.network.GigyaError;

import java.util.HashMap;
import java.util.Map;

public class PhoneVerificationResolver<A extends GigyaAccount> extends Resolver<A> implements IPhoneVerificationResolver<A> {

    public static final String LOG_TAG = "PhoneVerificationResolver";

    public PhoneVerificationResolver(IBusinessApiService<A> businessApiService) {
        super(businessApiService);
    }

    @Override
    public void verifyCode(@NonNull final String regToken, @NonNull String verificationCode, @NonNull final String gigyaAssertion,
                           @NonNull String phvToken, @NonNull final VerificationCallback<A> verificationCallback) {
        GigyaLogger.debug(LOG_TAG, "verify code with " + verificationCode + ", gigyaAssertion: " + gigyaAssertion + ", phvToken: " + phvToken);
        _businessApiService.completePhoneVerification(gigyaAssertion, verificationCode, phvToken, new GigyaLoginCallback<TFACompleteVerificationModel>() {
            @Override
            public void onSuccess(TFACompleteVerificationModel model) {
                finalizeFlow(regToken, gigyaAssertion, model.getProviderAssertion(), verificationCallback);
            }

            @Override
            public void onError(GigyaError error) {
                verificationCallback.onError(error);
            }
        });
    }

    private void finalizeFlow(@NonNull final String regToken, @NonNull String gigyaAssertion, @NonNull String providerAssertion,
                              @NonNull final VerificationCallback<A> verificationCallback) {
        GigyaLogger.debug(LOG_TAG, "finalizeFlow with regToken: " + regToken + ", gigyaAssertion: " + gigyaAssertion + ", providerAssertion: " + providerAssertion);
        _businessApiService.finalizeTFA(regToken, gigyaAssertion, providerAssertion, new GigyaLoginCallback<GigyaApiResponse>() {
            @Override
            public void onSuccess(GigyaApiResponse redundant) {
                finalizeRegistration(regToken, verificationCallback);
            }

            @Override
            public void onError(GigyaError error) {
                verificationCallback.onError(error);
            }
        });
    }

    private void finalizeRegistration(@NonNull String regToken, @NonNull final VerificationCallback<A> verificationCallback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        params.put("include", "profile,data,emails,subscriptions,preferences");
        params.put("includeUserInfo", "true");
        _businessApiService.finalizeRegistration(params, new GigyaLoginCallback<A>() {
            @Override
            public void onSuccess(A account) {
                verificationCallback.onVerified(account);
            }

            @Override
            public void onError(GigyaError error) {
                verificationCallback.onError(error);
            }
        });
    }


    public interface VerificationCallback<A> {

        void onVerified(A account);

        void onError(GigyaError error);
    }
}
