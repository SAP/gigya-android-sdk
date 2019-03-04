package com.gigya.android.sdk.interruption.tfa;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.account.FinalizeRegistrationApi;
import com.gigya.android.sdk.api.tfa.TFAFinalizeApi;
import com.gigya.android.sdk.api.tfa.TFAGetProvidersApi;
import com.gigya.android.sdk.api.tfa.TFAInitApi;
import com.gigya.android.sdk.api.tfa.email.TFAGetEmailsApi;
import com.gigya.android.sdk.api.tfa.email.TFASendEmailVerificationCodeApi;
import com.gigya.android.sdk.api.tfa.phone.TFACompleteVerificationApi;
import com.gigya.android.sdk.api.tfa.phone.TFAGetRegisteredPhoneNumbersAPi;
import com.gigya.android.sdk.api.tfa.phone.TFASendVerificationCodeApi;
import com.gigya.android.sdk.api.tfa.totp.TFATOTPRegisterApi;
import com.gigya.android.sdk.api.tfa.totp.TFATOTPVerifyApi;
import com.gigya.android.sdk.interruption.GigyaResolver;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.tfa.TFACompleteVerificationResponse;
import com.gigya.android.sdk.model.tfa.TFAEmail;
import com.gigya.android.sdk.model.tfa.TFAGetEmailsResponse;
import com.gigya.android.sdk.model.tfa.TFAGetRegisteredPhoneNumbersResponse;
import com.gigya.android.sdk.model.tfa.TFAInitResponse;
import com.gigya.android.sdk.model.tfa.TFAProvider;
import com.gigya.android.sdk.model.tfa.TFAProvidersResponse;
import com.gigya.android.sdk.model.tfa.TFARegisteredPhone;
import com.gigya.android.sdk.model.tfa.TFATotpRegisterResponse;
import com.gigya.android.sdk.model.tfa.TFAVerificationCodeResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.ArrayList;
import java.util.List;

public class TFAResolver<T extends GigyaAccount> extends GigyaResolver<T> {

    private static final String LOG_TAG = "TFAResolver";

    private String regToken;
    private GigyaResponse originalResponse;

    private String gigyaAssertion;

    // Phone.
    private String phvToken;
    // TOTP
    private String sctToken;

    private ArrayList<String> providerList = new ArrayList<>(2);

    public TFAResolver(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager,
                       GigyaLoginCallback<T> loginCallback) {
        super(networkAdapter, sessionManager, accountManager, loginCallback);
    }

    public void setOriginalData(String regToken, GigyaResponse response) {
        this.regToken = regToken;
        this.originalResponse = response;
    }

    public void init() {
        new TFAGetProvidersApi(networkAdapter, sessionManager)
                .call(this.regToken, new GigyaCallback<TFAProvidersResponse>() {
                    @Override
                    public void onSuccess(TFAProvidersResponse obj) {
                        evaluateProviders(obj);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    private void evaluateProviders(TFAProvidersResponse res) {
        if (res.getActiveProviders().isEmpty()) {
            addProviders(res.getInactiveProviders());
            if (loginCallback.get() != null)
                loginCallback.get().onPendingTFARegistration(this.originalResponse, this);
        } else {
            addProviders(res.getActiveProviders());
            if (loginCallback.get() != null)
                loginCallback.get().onPendingTFAVerification(this.originalResponse, this);
        }
    }

    private void addProviders(List<TFAProvider> providers) {
        for (TFAProvider provider : providers) {
            providerList.add(provider.getName());
        }
    }

    public ArrayList<String> getProviders() {
        return providerList;
    }

    //region Phone

    public void registerPhone(final String phoneNumber, final String method) {
        new TFAInitApi(networkAdapter, sessionManager)
                .call(this.regToken, GigyaDefinitions.TFA.PHONE, "register", new GigyaCallback<TFAInitResponse>() {
                    @Override
                    public void onSuccess(TFAInitResponse obj) {
                        gigyaAssertion = obj.getGigyaAssertion();
                        sendVerificationCode(phoneNumber, method, false);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    public void verifyPhone() {
        new TFAInitApi(networkAdapter, sessionManager)
                .call(this.regToken, GigyaDefinitions.TFA.PHONE, "verify", new GigyaCallback<TFAInitResponse>() {
                    @Override
                    public void onSuccess(TFAInitResponse obj) {
                        gigyaAssertion = obj.getGigyaAssertion();
                        getRegisteredPhoneNumbers();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
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
                                forwardError(error);
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
                            forwardError(GigyaError.generalError());
                            // Shouldn't happen but hey...
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    public void submitPhoneCode(String code) {
        new TFACompleteVerificationApi(networkAdapter, sessionManager)
                .call(gigyaAssertion, phvToken, code, new GigyaCallback<TFACompleteVerificationResponse>() {
                    @Override
                    public void onSuccess(TFACompleteVerificationResponse obj) {
                        finalizeTFA(obj.getProviderAssertion());
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    //endregion

    //region TOTP

    public void registerTOTP() {
        new TFAInitApi(networkAdapter, sessionManager)
                .call(this.regToken, GigyaDefinitions.TFA.TOTP, "register", new GigyaCallback<TFAInitResponse>() {
                    @Override
                    public void onSuccess(TFAInitResponse obj) {
                        gigyaAssertion = obj.getGigyaAssertion();
                        getQrCode();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (loginCallback.get() != null) {
                            loginCallback.get().onError(error);
                        }
                    }
                });
    }

    private void getQrCode() {
        new TFATOTPRegisterApi(networkAdapter, sessionManager)
                .call(this.gigyaAssertion, new GigyaCallback<TFATotpRegisterResponse>() {
                    @Override
                    public void onSuccess(TFATotpRegisterResponse obj) {
                        sctToken = obj.getSctToken();
                        if (loginCallback.get() != null) {
                            loginCallback.get().onTOTPQrCodeAvailable(obj.getQrCode());
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

    public void submitTOTPCode(String code) {
        new TFATOTPVerifyApi(networkAdapter, sessionManager)
                .call(gigyaAssertion, code, sctToken, new GigyaCallback<TFACompleteVerificationResponse>() {
                    @Override
                    public void onSuccess(TFACompleteVerificationResponse obj) {
                        finalizeTFA(obj.getProviderAssertion());
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (loginCallback.get() != null) {
                            loginCallback.get().onError(error);
                        }
                    }
                });
    }

    public void verifyTOTP(final String code) {
        new TFAInitApi(networkAdapter, sessionManager)
                .call(regToken, GigyaDefinitions.TFA.TOTP, "verify", new GigyaCallback<TFAInitResponse>() {
                    @Override
                    public void onSuccess(TFAInitResponse obj) {
                        gigyaAssertion = obj.getGigyaAssertion();
                        submitTOTPCode(code);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (loginCallback.get() != null) {
                            loginCallback.get().onError(error);
                        }
                    }
                });
    }

    //endregion

    //region Email

    public void verifyEmail() {
        new TFAInitApi(networkAdapter, sessionManager)
                .call(this.regToken, GigyaDefinitions.TFA.EMAIL, "verify", new GigyaCallback<TFAInitResponse>() {
                    @Override
                    public void onSuccess(TFAInitResponse obj) {
                        gigyaAssertion = obj.getGigyaAssertion();
                        getEmails();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    private void getEmails() {
        new TFAGetEmailsApi(networkAdapter, sessionManager)
                .call(this.gigyaAssertion, new GigyaCallback<TFAGetEmailsResponse>() {
                    @Override
                    public void onSuccess(TFAGetEmailsResponse obj) {
                        final List<TFAEmail> emails = obj.getEmails();
                        if (emails.isEmpty()) {
                            forwardError(GigyaError.generalError());
                            return;
                        }
                        final String emailID = emails.get(0).getId();
                        sendEmailVerificationCode(emailID);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    private void sendEmailVerificationCode(String emailID) {
        new TFASendEmailVerificationCodeApi(networkAdapter, sessionManager);
    }

    //endregion

    //region Finalize

    private void finalizeTFA(String providerAssertion) {
        new TFAFinalizeApi(networkAdapter, sessionManager)
                .call(gigyaAssertion, providerAssertion, regToken, new GigyaCallback<GigyaResponse>() {
                    @Override
                    public void onSuccess(GigyaResponse obj) {
                        finalizeRegistration();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    private void finalizeRegistration() {
        if (loginCallback.get() != null) {
            GigyaLogger.debug(LOG_TAG, "Sending finalize registration");
            new FinalizeRegistrationApi<T>(networkAdapter, sessionManager, accountManager)
                    .call(regToken, loginCallback.get(), new Runnable() {
                        @Override
                        public void run() {
                            // Nullify all relevant fields of the resolver.
                            nullify();
                        }
                    });
        } else {
            GigyaLogger.error(LOG_TAG, "Login callback reference is null -> Not sending finalize registration");
        }
    }

    private void nullify() {
        regToken = null;
        gigyaAssertion = null;
        phvToken = null;
        sctToken = null;
        originalResponse = null;
        providerList.clear();
        loginCallback.clear();
    }

    //endregion

    @Override
    public void cancel() {
        if (loginCallback.get() != null) {
            loginCallback.get().onOperationCancelled();
        }
        nullify();
    }
}
