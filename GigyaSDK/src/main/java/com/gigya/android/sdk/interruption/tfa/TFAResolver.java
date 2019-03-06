package com.gigya.android.sdk.interruption.tfa;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.account_dep.FinalizeRegistrationApi;
import com.gigya.android.sdk.api.tfa_dep.TFAFinalizeApi;
import com.gigya.android.sdk.api.tfa_dep.TFAGetProvidersApi;
import com.gigya.android.sdk.api.tfa_dep.TFAInitApi;
import com.gigya.android.sdk.api.tfa_dep.email.TFAGetEmailsApi;
import com.gigya.android.sdk.api.tfa_dep.email.TFASendEmailVerificationCodeApi;
import com.gigya.android.sdk.api.tfa_dep.phone.TFACompleteVerificationApi;
import com.gigya.android.sdk.api.tfa_dep.phone.TFAGetRegisteredPhoneNumbersAPi;
import com.gigya.android.sdk.api.tfa_dep.phone.TFASendVerificationCodeApi;
import com.gigya.android.sdk.api.tfa_dep.totp.TFATOTPRegisterApi;
import com.gigya.android.sdk.api.tfa_dep.totp.TFATOTPVerifyApi;
import com.gigya.android.sdk.interruption.GigyaResolver;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.tfa.TFACompleteVerificationModel;
import com.gigya.android.sdk.model.tfa.TFAEmail;
import com.gigya.android.sdk.model.tfa.TFAGetEmailsModel;
import com.gigya.android.sdk.model.tfa.TFAGetRegisteredPhoneNumbersModel;
import com.gigya.android.sdk.model.tfa.TFAInitModel;
import com.gigya.android.sdk.model.tfa.TFAProvider;
import com.gigya.android.sdk.model.tfa.TFAProvidersModel;
import com.gigya.android.sdk.model.tfa.TFARegisteredPhone;
import com.gigya.android.sdk.model.tfa.TFATotpRegisterModel;
import com.gigya.android.sdk.model.tfa.TFAVerificationCodeModel;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.ArrayList;
import java.util.List;

public class TFAResolver<T extends GigyaAccount> extends GigyaResolver<T> {

    private static final String LOG_TAG = "TFAResolver";

    private String regToken;
    private GigyaApiResponse originalResponse;

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

    public void setOriginalData(String regToken, GigyaApiResponse response) {
        this.regToken = regToken;
        this.originalResponse = response;
    }

    public void init() {
        new TFAGetProvidersApi(networkAdapter, sessionManager)
                .call(this.regToken, new GigyaCallback<TFAProvidersModel>() {
                    @Override
                    public void onSuccess(TFAProvidersModel obj) {
                        evaluateProviders(obj);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    private void evaluateProviders(TFAProvidersModel res) {
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
                .call(this.regToken, GigyaDefinitions.TFA.PHONE, "register", new GigyaCallback<TFAInitModel>() {
                    @Override
                    public void onSuccess(TFAInitModel obj) {
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
                .call(this.regToken, GigyaDefinitions.TFA.PHONE, "verify", new GigyaCallback<TFAInitModel>() {
                    @Override
                    public void onSuccess(TFAInitModel obj) {
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
                        , isVerify, new GigyaCallback<TFAVerificationCodeModel>() {
                            @Override
                            public void onSuccess(TFAVerificationCodeModel obj) {
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
                .call(this.gigyaAssertion, new GigyaCallback<TFAGetRegisteredPhoneNumbersModel>() {
                    @Override
                    public void onSuccess(TFAGetRegisteredPhoneNumbersModel obj) {
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
                .call(gigyaAssertion, phvToken, code, new GigyaCallback<TFACompleteVerificationModel>() {
                    @Override
                    public void onSuccess(TFACompleteVerificationModel obj) {
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
                .call(this.regToken, GigyaDefinitions.TFA.TOTP, "register", new GigyaCallback<TFAInitModel>() {
                    @Override
                    public void onSuccess(TFAInitModel obj) {
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
                .call(this.gigyaAssertion, new GigyaCallback<TFATotpRegisterModel>() {
                    @Override
                    public void onSuccess(TFATotpRegisterModel obj) {
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
                .call(gigyaAssertion, code, sctToken, new GigyaCallback<TFACompleteVerificationModel>() {
                    @Override
                    public void onSuccess(TFACompleteVerificationModel obj) {
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
                .call(regToken, GigyaDefinitions.TFA.TOTP, "verify", new GigyaCallback<TFAInitModel>() {
                    @Override
                    public void onSuccess(TFAInitModel obj) {
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
                .call(this.regToken, GigyaDefinitions.TFA.EMAIL, "verify", new GigyaCallback<TFAInitModel>() {
                    @Override
                    public void onSuccess(TFAInitModel obj) {
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
                .call(this.gigyaAssertion, new GigyaCallback<TFAGetEmailsModel>() {
                    @Override
                    public void onSuccess(TFAGetEmailsModel obj) {
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
                .call(gigyaAssertion, providerAssertion, regToken, new GigyaCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse obj) {
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
