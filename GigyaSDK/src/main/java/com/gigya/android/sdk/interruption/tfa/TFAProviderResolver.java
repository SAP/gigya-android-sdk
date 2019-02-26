package com.gigya.android.sdk.interruption.tfa;

import android.support.v4.util.ArrayMap;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.tfa.TFAGetProvidersApi;
import com.gigya.android.sdk.interruption.GigyaResolver;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.tfa.TFAProvider;
import com.gigya.android.sdk.model.tfa.TFAProvidersResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.List;

public class TFAProviderResolver<T extends GigyaAccount> extends GigyaResolver {

    private GigyaLoginCallback<T> loginCallback;

    private String regToken;
    private final ArrayMap<String, GigyaResolver> resolverArrayMap;

    public TFAProviderResolver(Configuration configuration, NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager,
                               ArrayMap<String, GigyaResolver> resolverArrayMap,
                               GigyaLoginCallback<T> loginCallback) {
        super(configuration, networkAdapter, sessionManager, accountManager);
        this.loginCallback = loginCallback;
        this.resolverArrayMap = resolverArrayMap;
    }

    public void setRegToken(String regToken) {
        this.regToken = regToken;
    }

    public void getProviders() {
        new TFAGetProvidersApi(networkAdapter, sessionManager)
                .call(this.regToken, new GigyaCallback<TFAProvidersResponse>() {
                    @Override
                    public void onSuccess(TFAProvidersResponse obj) {
                        evaluateProviders(obj);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        loginCallback.onError(error);
                    }
                });
    }

    private void evaluateProviders(TFAProvidersResponse res) {
        if (res.getActiveProviders().isEmpty()) {
            addResolvers(res.getInactiveProviders());
            loginCallback.onPendingTFARegistration(res.getInactiveProviders());
        } else {
            addResolvers(res.getActiveProviders());
            loginCallback.onPendingTFAVerification(res.getActiveProviders());
        }
    }

    private void addResolvers(List<TFAProvider> providers) {
        for (TFAProvider provider : providers) {
            switch (provider.getName()) {
                case "gigyaPhone":
                    this.resolverArrayMap.put(GigyaResolver.TFA_PHONE,
                            new TFAPhoneResolver(configuration, networkAdapter, sessionManager, accountManager, regToken));
                    break;
                case "gigyaTotp":
                    this.resolverArrayMap.put(GigyaResolver.TFA_TOTP,
                            new TFATotpResolver(configuration, networkAdapter, sessionManager, accountManager, regToken));
                    break;
            }

        }
    }
//
//    public void resolveForPhoneRegistration(String provider, String phoneNumber, final String method) {
//        this.phoneNumber = Long.parseLong(phoneNumber.replace("+", ""));
//        new TFAInitApi(configuration, networkAdapter, sessionManager, accountManager)
//                .call(this.regToken, provider, mode, new GigyaCallback<TFAInitResponse>() {
//                    @Override
//                    public void onSuccess(TFAInitResponse obj) {
//                        gigyaAssertion = obj.getGigyaAssertion();
//                        sendVerificationCode(method);
//
//                    }
//
//                    @Override
//                    public void onError(GigyaError error) {
//                        loginCallback.onError(error);
//                    }
//                });
//    }


//    private void getRegisteredPhoneNumbers() {
//        new TFAGetRegisteredPhoneNumbersAPi(configuration, networkAdapter, sessionManager, accountManager)
//                .call(this.gigyaAssertion, new GigyaCallback<TFAGetRegisteredPhoneNumbersResponse>() {
//                    @Override
//                    public void onSuccess(TFAGetRegisteredPhoneNumbersResponse obj) {
//
//                    }
//
//                    @Override
//                    public void onError(GigyaError error) {
//
//                    }
//                });
//    }

//    private void sendVerificationCode(String method) {
//        new TFASendVerificationCodeApi(configuration, networkAdapter, sessionManager, accountManager)
//                .call(gigyaAssertion, this.phoneNumber, method,
//                        "en" /* SDK Hardcoded to English. */
//                        , new GigyaCallback<TFAVerificationCodeResponse>() {
//                            @Override
//                            public void onSuccess(TFAVerificationCodeResponse obj) {
//                                phvToken = obj.getPhvToken();
//                            }
//
//                            @Override
//                            public void onError(GigyaError error) {
//                                loginCallback.onError(error);
//                            }
//                        }
//                );
//    }

//    public void completeVerification(String code) {
//        new TFACompleteVerificationApi(configuration, networkAdapter, sessionManager, accountManager)
//                .call(gigyaAssertion, phvToken, code, new GigyaCallback<TFACompleteVerificationResponse>() {
//                    @Override
//                    public void onSuccess(TFACompleteVerificationResponse obj) {
//                        finalizeTFA(obj.getProviderAssertion());
//                    }
//
//                    @Override
//                    public void onError(GigyaError error) {
//                        loginCallback.onError(error);
//                    }
//                });
//    }
//
//    private void finalizeTFA(String providerAssertion) {
//        new TFAFinalizeApi(configuration, networkAdapter, sessionManager, accountManager)
//                .call(gigyaAssertion, providerAssertion, regToken, new GigyaCallback<GigyaResponse>() {
//                    @Override
//                    public void onSuccess(GigyaResponse obj) {
//                        finalizeRegistration();
//                    }
//
//                    @Override
//                    public void onError(GigyaError error) {
//                        loginCallback.onError(error);
//                    }
//                });
//    }

//    private void finalizeRegistration() {
//        new FinalizeRegistrationApi<T>(configuration, networkAdapter, sessionManager, accountManager)
//                .call(regToken, loginCallback);
//    }

//    //region Getters
//
//    public String getRegToken() {
//        return regToken;
//    }
//
//    public List<TFAProvider> getActiveProviders() {
//        return activeProviders;
//    }
//
//    public List<TFAProvider> getInactiveProviders() {
//        return inactiveProviders;
//    }
//
//    //endregion
}
