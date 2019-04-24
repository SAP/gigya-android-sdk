package com.gigya.android.sdk.interruption.tfa;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.ApiService;
import com.gigya.android.sdk.api.IApiObservable;
import com.gigya.android.sdk.api.IApiService;
import com.gigya.android.sdk.interruption.GigyaResolver;
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
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.RestAdapter;
import com.gigya.android.sdk.session.ISessionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GigyaTFAResolver<A extends GigyaAccount> extends GigyaResolver<A> {

    private static final String LOG_TAG = "GigyaTFAResolver";

    public GigyaTFAResolver(Config config, ISessionService sessionService, IApiService apiService, IApiObservable observable,
                            GigyaApiResponse originalResponse, GigyaLoginCallback<A> loginCallback) {
        super(config, sessionService, apiService, observable, originalResponse, loginCallback);
    }

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
     * Forward first interruption to end user after providers are available (active and inactive).
     */
    protected abstract void forwardInitialInterruption();

    /**
     * Initializing the TFA resolver.
     * Will initiate a "accounts.tfa.getProviders" request to validate which providers are available for us
     * for the continuation of the flow.
     */
    @Override
    public void start() {
        GigyaLogger.debug(LOG_TAG, "Resolver init - request providers");
        // Request providers. Populate active & inactive on response.
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", _regToken);
        final GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_TFA_GET_PROVIDERS, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    final TFAProvidersModel parsed = response.parseTo(TFAProvidersModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        forwardError(GigyaError.fromResponse(response));
                        return;
                    }
                    GigyaTFAResolver.this.activeProviders = parsed.getActiveProviders();
                    GigyaTFAResolver.this.inactiveProviders = parsed.getInactiveProviders();
                    forwardInitialInterruption();
                } else {
                    forwardError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                forwardError(gigyaError);
            }
        });
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
        // Generate & send request
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", _regToken);
        params.put("provider", provider);
        params.put("mode", mode);
        final GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_TFA_INIT, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    final TFAInitModel parsed = response.parseTo(TFAInitModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        forwardError(GigyaError.fromResponse(response));
                        return;
                    }
                    GigyaTFAResolver.this.gigyaAssertion = parsed.getGigyaAssertion();
                    onInit(provider, mode, arguments);
                } else {
                    forwardError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                forwardError(gigyaError);
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
    private void completeVerification(String api, final Map<String, Object> params) {
        GigyaLogger.debug(LOG_TAG, "completeVerification: with api = " + api);
        // Generate & send request
        final GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, api, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    final TFACompleteVerificationModel parsed = response.parseTo(TFACompleteVerificationModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        forwardError(GigyaError.fromResponse(response));
                        return;
                    }
                    finalizeTFA(parsed.getProviderAssertion());
                } else {
                    forwardError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                forwardError(gigyaError);
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
        // Generate & send request
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", _regToken);
        params.put("gigyaAssertion", this.gigyaAssertion);
        params.put("providerAssertion", providerAssertion);
        GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_TFA_FINALIZE, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                finalizeRegistration();
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                forwardError(gigyaError);
            }
        });
    }

    /**
     * Finalizing the flow.
     */
    private void finalizeRegistration() {
        if (isAttached()) {
            GigyaLogger.debug(LOG_TAG, "finalizeRegistration: ");
            // Params.
            final Map<String, Object> params = new HashMap<>();
            params.put("regToken", _regToken);
            params.put("include", "profile,data,emails,subscriptions,preferences");
            params.put("includeUserInfo", "true");
            // Api.
            final String api = GigyaDefinitions.API.API_FINALIZE_REGISTRATION;
            // Notify observer.
            _observable.send(api, params, _loginCallback.get());
        }
    }

    //endregion

    //region TOTP

    private String sctToken;

    /**
     * Register TOTP. Will request the QR code needed for authenticator application.
     */
    protected void registerTotp() {
        initTFA(GigyaDefinitions.TFA.TOTP, "register", null);
    }

    /**
     * Verify TOTP using generated authentication code.
     *
     * @param code Authentication code.
     */
    protected void verifyTotp(final String code) {
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
        GigyaLogger.debug(LOG_TAG, "getTotpQRCode: ");
        // Generate & send request
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        final GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_TFA_TOTP_REGISTER, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    final TFATotpRegisterModel parsed = response.parseTo(TFATotpRegisterModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        forwardError(GigyaError.fromResponse(response));
                        return;
                    }
                    GigyaTFAResolver.this.sctToken = parsed.getSctToken();
                    if (isAttached()) {
                        _loginCallback.get().onTotpTFAQrCodeAvailable(parsed.getQrCode());
                    }
                } else {
                    forwardError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                forwardError(gigyaError);
            }
        });
    }

    /**
     * Submit authenticator application generated code in order to finalize the login.
     *
     * @param code Authentication code.
     */
    protected void submitTotpCode(String code) {
        GigyaLogger.debug(LOG_TAG, "submitTotpCode: with code = " + code);
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", this.gigyaAssertion);
        params.put("code", code);
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
    protected void registerPhone(final String number, final String method) {
        final Map<String, String> arguments = new HashMap<>();
        arguments.put("number", number);
        arguments.put("method", method);
        initTFA(GigyaDefinitions.TFA.PHONE, "register", arguments);
    }

    /**
     * Request phone number verification.
     */
    protected void verifyPhone() {
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
        // Generate & send request
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", this.gigyaAssertion);
        GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_TFA_PHONE_GET_REGISTERED_NUMBERS, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    final TFAGetRegisteredPhoneNumbersModel parsed = response.parseTo(TFAGetRegisteredPhoneNumbersModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        forwardError(GigyaError.fromResponse(response));
                        return;
                    }
                    if (isAttached()) {
                        _loginCallback.get().onRegisteredTFAPhoneNumbers(parsed.getPhones());
                    }
                } else {
                    forwardError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                forwardError(gigyaError);
            }
        });
    }

    /**
     * Trigger verification code to be sent to target phone number or phone ID (for verify state).
     *
     * @param numberOrId Phone number of phone ID.
     * @param method     Verification code receive method (sms/voice).
     * @param isVerify   is "verify" mode?
     */
    protected void sendPhoneVerificationCode(final String numberOrId, final String method, final boolean isVerify) {
        GigyaLogger.debug(LOG_TAG, "sendPhoneVerificationCode: method = " + method + " isVerify = " + String.valueOf(isVerify));
        // Generate & send request
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", this.gigyaAssertion);
        params.put(isVerify ? "phoneID" : "phone", numberOrId);
        params.put("method", method);
        params.put("lang", "eng");
        GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_TFA_PHONE_SEND_VERIFICATION_CODE, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    final TFAVerificationCodeModel parsed = response.parseTo(TFAVerificationCodeModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        forwardError(GigyaError.fromResponse(response));
                        return;
                    }
                    GigyaTFAResolver.this.phvToken = parsed.getPhvToken();
                    if (isAttached()) {
                        _loginCallback.get().onPhoneTFAVerificationCodeSent();
                    }
                } else {
                    forwardError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                forwardError(gigyaError);
            }
        });
    }

    /**
     * Submit received code (via sms or voice) in order to finalize the login.
     *
     * @param code Authentication code.
     */
    protected void submitPhoneCode(String code) {
        GigyaLogger.debug(LOG_TAG, "submitPhoneCode: with code = " + code);
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", this.gigyaAssertion);
        params.put("code", code);
        params.put("phvToken", phvToken);
        completeVerification(GigyaDefinitions.API.API_TFA_PHONE_COMPLETE_VERIFICATION, params);
    }

    //endregion

    //region EMAIL

    protected void verifyEmail() {
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
        GigyaLogger.debug(LOG_TAG, "getVerifiedEmails: ");
        // Generate & send request
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", this.gigyaAssertion);
        GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_TFA_EMAIL_GET_EMAILS, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    final TFAGetEmailsModel parsed = response.parseTo(TFAGetEmailsModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        forwardError(GigyaError.fromResponse(response));
                        return;
                    }
                    final List<TFAEmail> emails = parsed.getEmails();
                    if (emails.isEmpty()) {
                        forwardError(GigyaError.generalError());
                        return;
                    }
                    // Forward emails.
                    if (isAttached()) {
                        _loginCallback.get().onEmailTFAAddressesAvailable(emails);
                    }
                } else {
                    forwardError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                forwardError(gigyaError);
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
    protected void verifyWithEmail(TFAEmail tfaEmail) {
        GigyaLogger.debug(LOG_TAG, "verifyWithEmail: " + tfaEmail.getObfuscated() + " with id = " + tfaEmail.getId());
        // Generate & send request
        final Map<String, Object> params = new HashMap<>();
        params.put("emailID", tfaEmail.getId());
        params.put("gigyaAssertion", this.gigyaAssertion);
        params.put("lang", "eng");
        GigyaApiRequest request = GigyaApiRequest.newInstance(_config, _sessionService, GigyaDefinitions.API.API_TFA_EMAIL_SEND_VERIFICATION_CODE, params, RestAdapter.POST);
        _apiService.send(request, false, new ApiService.IApiServiceResponse() {
            @Override
            public void onApiSuccess(GigyaApiResponse response) {
                if (response.getErrorCode() == 0) {
                    final TFAVerificationCodeModel parsed = response.parseTo(TFAVerificationCodeModel.class);
                    if (parsed == null) {
                        // Parsing error.
                        forwardError(GigyaError.fromResponse(response));
                        return;
                    }
                    GigyaTFAResolver.this.phvToken = parsed.getPhvToken();
                    if (isAttached()) {
                        _loginCallback.get().onEmailTFAVerificationEmailSent();
                    }
                } else {
                    forwardError(GigyaError.fromResponse(response));
                }
            }

            @Override
            public void onApiError(GigyaError gigyaError) {
                forwardError(gigyaError);
            }
        });
    }

    /**
     * Submit received code via email in order to finalize the login.
     *
     * @param code Authentication code.
     */
    protected void submitEmailCode(String code) {
        GigyaLogger.debug(LOG_TAG, "sendEmailVerificationCode: with code = " + code);
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", this.gigyaAssertion);
        params.put("code", code);
        params.put("phvToken", phvToken);
        completeVerification(GigyaDefinitions.API.API_TFA_EMAIL_COMPLETE_VERIFICATION, params);
    }

    //endregion

}
