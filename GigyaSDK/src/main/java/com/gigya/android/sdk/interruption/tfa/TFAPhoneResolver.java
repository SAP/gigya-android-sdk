package com.gigya.android.sdk.interruption.tfa;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.tfa.TFAInitApi;
import com.gigya.android.sdk.api.tfa.phone.TFASendVerificationCodeApi;
import com.gigya.android.sdk.interruption.GigyaResolver;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.tfa.TFAInitResponse;
import com.gigya.android.sdk.model.tfa.TFAVerificationCodeResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.lang.ref.WeakReference;

public class TFAPhoneResolver<T extends GigyaAccount> extends GigyaResolver {

    private WeakReference<GigyaLoginCallback<T>> loginCallback;
    private final String regToken;
    private String gigyaAssertion;
    private String phvToken;

    public String getGigyaAssertion() {
        return gigyaAssertion;
    }

    public String getPhvToken() {
        return phvToken;
    }

    TFAPhoneResolver(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager,
                     String regToken, WeakReference<GigyaLoginCallback<T>> loginCallback) {
        super(networkAdapter, sessionManager, accountManager);
        this.regToken = regToken;
        this.loginCallback = loginCallback;
    }

    public void register(final String phoneNumber, final String method) {
        new TFAInitApi(networkAdapter, sessionManager)
                .call(this.regToken, "gigyaPhone", "register", new GigyaCallback<TFAInitResponse>() {
                    @Override
                    public void onSuccess(TFAInitResponse obj) {
                        gigyaAssertion = obj.getGigyaAssertion();
                        sendVerificationCode(phoneNumber, method);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (loginCallback.get() != null) {
                            loginCallback.get().onError(error);
                        }
                    }
                });
    }

    public void verify() {

    }

    private void sendVerificationCode(String phoneNumber, String method) {
        final long phone = Long.parseLong(phoneNumber);
        new TFASendVerificationCodeApi(networkAdapter, sessionManager)
                .call(gigyaAssertion, phone, method,
                        "en" /* SDK Hardcoded to English. */
                        , new GigyaCallback<TFAVerificationCodeResponse>() {
                            @Override
                            public void onSuccess(TFAVerificationCodeResponse obj) {
                                phvToken = obj.getPhvToken();
                            }

                            @Override
                            public void onError(GigyaError error) {
                                if (loginCallback.get() != null) {
                                    loginCallback.get().onError(error);
                                }
                            }
                        }
                );
    }

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

}
