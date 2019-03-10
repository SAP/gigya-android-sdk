package com.gigya.android.sdk.api.interruption;

import android.support.annotation.Nullable;
import android.support.v4.util.Pair;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.model.tfa.TFACompleteVerificationModel;
import com.gigya.android.sdk.model.tfa.TFAEmail;
import com.gigya.android.sdk.model.tfa.TFAGetEmailsModel;
import com.gigya.android.sdk.model.tfa.TFAGetRegisteredPhoneNumbersModel;
import com.gigya.android.sdk.model.tfa.TFAInitModel;
import com.gigya.android.sdk.model.tfa.TFAProvider;
import com.gigya.android.sdk.model.tfa.TFAProvidersModel;
import com.gigya.android.sdk.model.tfa.TFATotpRegisterModel;
import com.gigya.android.sdk.model.tfa.TFAVerificationCodeModel;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.services.ApiService;
import com.gigya.android.sdk.utils.ObjectUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GigyaTFAResolver<A extends GigyaAccount> extends GigyaResolver<A> {

    private static final String LOG_TAG = "GigyaTFAResolver";

    private List<TFAProvider> activeProviders;

    public List<TFAProvider> getActiveProviders() {
        return activeProviders;
    }

    private List<TFAProvider> inactiveProviders;

    public List<TFAProvider> getInactiveProviders() {
        return inactiveProviders;
    }

    private String gigyaAssertion;

    @Override
    public void clear() {
        GigyaLogger.debug(LOG_TAG, "clear: Nullity data");
        this.phvToken = null;
        this.gigyaAssertion = null;
        if (this.inactiveProviders != null) {
            this.inactiveProviders.clear();
        }
        if (this.activeProviders != null) {
            this.activeProviders.clear();
        }
        this.sctToken = null;
        if (isAttached()) {
            _loginCallback.clear();
        }
    }

    /**
     * Initializing the TFA resolver.
     * Will initiate a "accounts.tfa.getProviders" request to validate which providers are available for us
     * for the continuation of the flow.
     */
    @Override
    public void init(ApiService<A> apiService, GigyaApiResponse originalResponse, GigyaLoginCallback<? extends GigyaAccount> loginCallback) {
        super.init(apiService, originalResponse, loginCallback);
        GigyaLogger.debug(LOG_TAG, "Resolver init - request providers");
        // Request providers. Populate active & inactive on response.
        _apiService.send(GigyaDefinitions.API.API_TFA_GET_PROVIDERS,
                ObjectUtils.mapOf(Collections.singletonList(new Pair<String, Object>("regToken", _regToken))),
                TFAProvidersModel.class, new GigyaCallback<TFAProvidersModel>() {
                    @Override
                    public void onSuccess(TFAProvidersModel obj) {
                        // Populate providers.
                        GigyaTFAResolver.this.activeProviders = obj.getActiveProviders();
                        GigyaTFAResolver.this.inactiveProviders = obj.getInactiveProviders();
                        forwardInitialInterruption();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    /**
     * Forward first interruption to end user after providers are available (active & inactive).    sad
     */
    private void forwardInitialInterruption() {
        if (isAttached()) {
            final int errorCode = _originalResponse.getErrorCode();
            if (errorCode == GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION) {
                _loginCallback.get().onPendingTFARegistration(_originalResponse, this);
            } else if (errorCode == GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION) {
                _loginCallback.get().onPendingTFAVerification(_originalResponse, this);
            } else {
                GigyaLogger.error(LOG_TAG, "forwardInitialInterruption: Error does not meet interruption requirements. Sending general to avoid overflow");
                forwardError(GigyaError.generalError());
            }
        }
    }

    //region GENERAL

    /**
     * Initialize TFA flow.
     *
     * @param provider  Requested TFA provider
     * @param mode      Token mode (register/verify).
     * @param arguments Additional arguments
     */
    private void initTFA(final String provider, final String mode, final Map<String, String> arguments) {
        GigyaLogger.debug(LOG_TAG, "initTFA: provider = " + provider + " mode = " + mode);
        _apiService.send(GigyaDefinitions.API.API_TFA_INIT,
                ObjectUtils.mapOf(Arrays.asList(
                        new Pair<String, Object>("regToken", _regToken),
                        new Pair<String, Object>("provider", provider),
                        new Pair<String, Object>("mode", mode))),
                TFAInitModel.class, new GigyaCallback<TFAInitModel>() {
                    @Override
                    public void onSuccess(TFAInitModel obj) {
                        GigyaTFAResolver.this.gigyaAssertion = obj.getGigyaAssertion();
                        onInit(provider, mode, arguments);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    /**
     * InitTFA request success. Vary tasks according to provider & mode.
     *
     * @param provider  Gigya TFA provider.
     * @param mode      Token mode (register/verify).
     * @param arguments Additional arguments
     */
    private void onInit(String provider, String mode, @Nullable Map<String, String> arguments) {
        switch (provider) {
            case GigyaDefinitions.TFA.TOTP:
                onInitTotp(mode, arguments);
                break;
            case GigyaDefinitions.TFA.PHONE:
                onInitPhone(mode, arguments);
                break;
            case GigyaDefinitions.TFA.EMAIL:
                onInitEmail();
                break;
        }
    }

    /**
     * Complete TFA verification (for multiple providers).
     * First step before finalizing the registration.
     *
     * @param api    Request API.
     * @param params Request parameters.
     */
    private void completeVerification(String api, Map<String, Object> params) {
        _apiService.send(api, params, TFACompleteVerificationModel.class, new GigyaCallback<TFACompleteVerificationModel>() {
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

    /**
     * Finalize the TFA flow once a provider assertion is available.
     *
     * @param providerAssertion Gigya TFA provider assertion token.
     */
    private void finalizeTFA(String providerAssertion) {
        GigyaLogger.debug(LOG_TAG, "finalizeTFA: finalize with pa = " + providerAssertion);
        _apiService.send(GigyaDefinitions.API.API_TFA_FINALIZE,
                ObjectUtils.mapOf(Arrays.asList(
                        new Pair<String, Object>("regToken", _regToken),
                        new Pair<String, Object>("gigyaAssertion", this.gigyaAssertion),
                        new Pair<String, Object>("providerAssertion", providerAssertion))),
                GigyaApiResponse.class,
                new GigyaCallback<GigyaApiResponse>() {
                    @Override
                    public void onSuccess(GigyaApiResponse obj) {
                        finalizeRegistration();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                }
        );
    }

    /**
     * Finalizing the flow.
     */
    private void finalizeRegistration() {
        if (isAttached()) {
            GigyaLogger.debug(LOG_TAG, "finalizeRegistration: ");
            _apiService.finalizeRegistration(_regToken, _loginCallback.get());
        }
    }


    //endregion

    //region TOTP

    private String sctToken;

    /**
     * Register TOTP. Will request the QR code needed for authenticator application.
     */
    public void registerTotp() {
        initTFA(GigyaDefinitions.TFA.TOTP, "register", null);
    }

    /**
     * Verify TOTP using generated authentication code.
     *
     * @param code Authentication code.
     */
    public void verifyTotp(final String code) {
        final Map<String, String> arguments = new HashMap<>();
        arguments.put("code", code);
        initTFA(GigyaDefinitions.TFA.TOTP, "register", arguments);
    }

    /**
     * On TFA initialization for TOTP.
     *
     * @param mode      Token mode (register/verify).
     * @param arguments Optional arguments map.
     */
    private void onInitTotp(String mode, @Nullable Map<String, String> arguments) {
        switch (mode) {
            case "register":
                getTotpQRCode();
                break;
            case "verify":
                if (arguments != null) {
                    final String code = arguments.get("code");
                    submitTotpCode(code);
                }
                break;
        }
    }

    /**
     * Request "accounts.tfa.totp.register" that will result in the QR generation needed for the authenticator application.
     */
    private void getTotpQRCode() {
        _apiService.send(GigyaDefinitions.API.API_TFA_TOTP_REGISTER,
                ObjectUtils.mapOf(Collections.singletonList(new Pair<String, Object>("gigyaAssertion", this.gigyaAssertion))),
                TFATotpRegisterModel.class, new GigyaCallback<TFATotpRegisterModel>() {
                    @Override
                    public void onSuccess(TFATotpRegisterModel obj) {
                        GigyaTFAResolver.this.sctToken = obj.getSctToken();
                        if (isAttached()) {
                            _loginCallback.get().onTotpTFAQrCodeAvailable(obj.getQrCode());
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (isAttached()) {
                            _loginCallback.get().onError(error);
                        }
                    }
                });
    }

    /**
     * Submit authenticator application generated code in order to finalize the login.
     *
     * @param code Authentication code.
     */
    public void submitTotpCode(String code) {
        GigyaLogger.debug(LOG_TAG, "submitTotpCode: with code = " + code);
        Map<String, Object> params = ObjectUtils.mapOf(Arrays.asList(
                new Pair<String, Object>("gigyaAssertion", this.gigyaAssertion),
                new Pair<String, Object>("code", code)));
        if (this.sctToken != null) {
            params.put("sctToken", sctToken);
        }
        completeVerification(GigyaDefinitions.API.API_TFA_TOTP_VERIFY, params);
    }

    //endregion

    //region PHONE

    private String phvToken;

    /**
     * Register TOTP. Will request the QR code needed for authenticator application.
     */
    public void registerPhone(final String number, final String method) {
        final Map<String, String> arguments = new HashMap<>();
        arguments.put("number", number);
        arguments.put("method", method);
        initTFA(GigyaDefinitions.TFA.PHONE, "register", arguments);
    }

    /**
     * Request phone number verification.
     */
    public void verifyPhone() {
        initTFA(GigyaDefinitions.TFA.PHONE, "verify", null);
    }

    /**
     * On TFA initialization for PHONE.
     *
     * @param mode      Token mode (register/verify).
     * @param arguments Optional arguments map.
     */
    private void onInitPhone(String mode, Map<String, String> arguments) {
        switch (mode) {
            case "register":
                final String number = arguments.get("number");
                final String method = arguments.get("method");
                sendPhoneVerificationCode(number, method, false);
                break;
            case "verify":
                getRegisteredPhoneNumbers();
                break;
        }
    }

    private void getRegisteredPhoneNumbers() {
        GigyaLogger.debug(LOG_TAG, "getRegisteredPhoneNumbers: ");
        _apiService.send(GigyaDefinitions.API.API_TFA_PHONE_GET_REGISTERED_NUMBERS,
                ObjectUtils.mapOf(Collections.singletonList(new Pair<String, Object>("gigyaAssertion", this.gigyaAssertion))),
                TFAGetRegisteredPhoneNumbersModel.class, new GigyaCallback<TFAGetRegisteredPhoneNumbersModel>() {
                    @Override
                    public void onSuccess(TFAGetRegisteredPhoneNumbersModel obj) {
                        if (isAttached()) {
                            _loginCallback.get().onRegisteredTFAPhoneNumbers(obj.getPhones());
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (isAttached()) {
                            _loginCallback.get().onError(error);
                        }
                    }
                });
    }

    /**
     * Trigger verification code to be sent to target phone number or phone ID (for verify state).
     *
     * @param numberOfId Phone number of phone ID.
     * @param method     Verification code receive method (sms/voice).
     * @param isVerify   is "verify" mode?
     */
    public void sendPhoneVerificationCode(final String numberOfId, final String method, final boolean isVerify) {
        _apiService.send(GigyaDefinitions.API.API_TFA_PHONE_SEND_VERIFICATION_CODE,
                ObjectUtils.mapOf(Arrays.asList(
                        new Pair<String, Object>("gigyaAssertion", this.gigyaAssertion),
                        new Pair<String, Object>(isVerify ? "phoneID" : "phone", numberOfId),
                        new Pair<String, Object>("method", method),
                        new Pair<String, Object>("lang", "eng"))),
                TFAVerificationCodeModel.class, new GigyaCallback<TFAVerificationCodeModel>() {

                    @Override
                    public void onSuccess(TFAVerificationCodeModel obj) {
                        GigyaTFAResolver.this.phvToken = obj.getPhvToken();
                        if (isAttached()) {
                            _loginCallback.get().onPhoneTFAVerificationCodeSent();
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (isAttached()) {
                            _loginCallback.get().onError(error);
                        }
                    }
                });
    }

    /**
     * Submit received code (via sms or voice) in order to finalize the login.
     *
     * @param code Authentication code.
     */
    public void submitPhoneCode(String code) {
        GigyaLogger.debug(LOG_TAG, "submitPhoneCode: with code = " + code);
        Map<String, Object> params = ObjectUtils.mapOf(Arrays.asList(
                new Pair<String, Object>("gigyaAssertion", this.gigyaAssertion),
                new Pair<String, Object>("code", code),
                new Pair<String, Object>("phvToken", phvToken)));
        completeVerification(GigyaDefinitions.API.API_TFA_PHONE_COMPLETE_VERIFICATION, params);
    }

    //endregion

    //region EMAIL

    public void verifyEmail() {
        initTFA(GigyaDefinitions.TFA.EMAIL, "verify", null);
    }

    /**
     * On TFA initialization for Email.
     * Currently supports verification mode only.
     */
    private void onInitEmail() {
        getVerifiedEmails();
    }

    /**
     * Request Email TFA registered email addresses.
     */
    private void getVerifiedEmails() {
        GigyaLogger.debug(LOG_TAG, "getVerifiedEmails ");
        _apiService.send(GigyaDefinitions.API.API_TFA_EMAIL_GET_EMAILS,
                ObjectUtils.mapOf(Collections.singletonList(new Pair<String, Object>("gigyaAssertion", this.gigyaAssertion))),
                TFAGetEmailsModel.class, new GigyaCallback<TFAGetEmailsModel>() {
                    @Override
                    public void onSuccess(TFAGetEmailsModel obj) {
                        final List<TFAEmail> emails = obj.getEmails();
                        if (emails.isEmpty()) {
                            forwardError(GigyaError.generalError());
                            return;
                        }
                        // Forward emails.
                        if (isAttached()) {
                            _loginCallback.get().onEmailTFAAddressesAvailable(emails);
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                });
    }

    /**
     * Verify the selected email address.
     * This will result in an verification email sent to the selected email address that contains the verification code needed
     * to complete the verification process.
     *
     * @param tfaEmail Selected TFAEmail instance.
     */
    public void verifyWithEmail(TFAEmail tfaEmail) {
        GigyaLogger.debug(LOG_TAG, "verifyWithEmail: " + tfaEmail.getObfuscated() + " with id = " + tfaEmail.getId());
        _apiService.send(GigyaDefinitions.API.API_TFA_EMAIL_SEND_VERIFICATION_CODE,
                ObjectUtils.mapOf(Arrays.asList(
                        new Pair<String, Object>("emailID", tfaEmail.getId()),
                        new Pair<String, Object>("gigyaAssertion", this.gigyaAssertion),
                        new Pair<String, Object>("lang", "eng"))),
                TFAVerificationCodeModel.class, new GigyaCallback<TFAVerificationCodeModel>() {
                    @Override
                    public void onSuccess(TFAVerificationCodeModel obj) {
                        GigyaTFAResolver.this.phvToken = obj.getPhvToken();
                        if (isAttached()) {
                            _loginCallback.get().onEmailTFAVerificationEmailSent();
                        }
                    }

                    @Override
                    public void onError(GigyaError error) {
                        forwardError(error);
                    }
                }
        );
    }

    /**
     * Submit received code via email in order to finalize the login.
     *
     * @param code Authentication code.
     */
    public void sendEmailVerificationCode(String code) {
        GigyaLogger.debug(LOG_TAG, "sendEmailVerificationCode: with code = " + code);
        Map<String, Object> params = ObjectUtils.mapOf(Arrays.asList(
                new Pair<String, Object>("gigyaAssertion", this.gigyaAssertion),
                new Pair<String, Object>("code", code),
                new Pair<String, Object>("phvToken", phvToken)));
        completeVerification(GigyaDefinitions.API.API_TFA_EMAIL_COMPLETE_VERIFICATION, params);
    }

    //endregion

}
