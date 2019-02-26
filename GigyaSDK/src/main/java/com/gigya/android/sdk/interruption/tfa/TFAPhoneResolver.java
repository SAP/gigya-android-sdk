package com.gigya.android.sdk.interruption.tfa;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.tfa.TFAInitApi;
import com.gigya.android.sdk.api.tfa.phone.TFAGetRegisteredPhoneNumbersAPi;
import com.gigya.android.sdk.api.tfa.phone.TFASendVerificationCodeApi;
import com.gigya.android.sdk.interruption.GigyaResolver;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.tfa.TFAGetRegisteredPhoneNumbersResponse;
import com.gigya.android.sdk.model.tfa.TFAInitResponse;
import com.gigya.android.sdk.model.tfa.TFARegisteredPhone;
import com.gigya.android.sdk.model.tfa.TFAVerificationCodeResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.lang.ref.SoftReference;

public class TFAPhoneResolver<T extends GigyaAccount> extends GigyaResolver {

    private SoftReference<GigyaLoginCallback<T>> loginCallback;
    private final String regToken;
    private String gigyaAssertion;
    private String phvToken;

    String getGigyaAssertion() {
        return gigyaAssertion;
    }

    String getPhvToken() {
        return phvToken;
    }

    TFAPhoneResolver(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager,
                     String regToken, SoftReference<GigyaLoginCallback<T>> loginCallback) {
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
                        sendVerificationCode(phoneNumber, method, false);
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
        new TFAInitApi(networkAdapter, sessionManager)
                .call(this.regToken, "gigyaPhone", "verify", new GigyaCallback<TFAInitResponse>() {
                    @Override
                    public void onSuccess(TFAInitResponse obj) {
                        gigyaAssertion = obj.getGigyaAssertion();
                        getRegisteredPhoneNumbers();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (loginCallback.get() != null) {
                            loginCallback.get().onError(error);
                        }
                    }
                });
    }

    private void sendVerificationCode(String phoneNumber, String method, boolean isVerify) {
        new TFASendVerificationCodeApi(networkAdapter, sessionManager)
                .call(gigyaAssertion, phoneNumber, method,
                        "en" /* SDK Hardcoded to English. */
                        , isVerify, new GigyaCallback<TFAVerificationCodeResponse>() {
                            @Override
                            public void onSuccess(TFAVerificationCodeResponse obj) {
                                phvToken = obj.getPhvToken();
                                if (loginCallback.get() != null) {
                                    loginCallback.get().onPhoneTFAVerificationCodeSent();
                                }
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

    private void getRegisteredPhoneNumbers() {
        new TFAGetRegisteredPhoneNumbersAPi(networkAdapter, sessionManager)
                .call(this.gigyaAssertion, new GigyaCallback<TFAGetRegisteredPhoneNumbersResponse>() {
                    @Override
                    public void onSuccess(TFAGetRegisteredPhoneNumbersResponse obj) {
                        if (!obj.getPhones().isEmpty()) {
                            TFARegisteredPhone phone = obj.getPhones().get(0);
                            sendVerificationCode(phone.getId(), phone.getLastMethod(), true);
                        } else {
                            // Shouldn't happen but hey...
                            if (loginCallback.get() != null) {
                                loginCallback.get().onError(GigyaError.generalError());
                            }
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (loginCallback.get() != null) {
                            loginCallback.get().onError(error);
                        }
                    }
                });
    }

}
