package com.gigya.android.sdk.interruption.tfa;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.IBusinessApiService;
import com.gigya.android.sdk.interruption.GigyaFinalizeResolver;
import com.gigya.android.sdk.account.models.GigyaAccount;
import com.gigya.android.sdk.interruption.tfa.models.TFACompleteVerificationModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAEmail;
import com.gigya.android.sdk.interruption.tfa.models.TFAGetEmailsModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAGetRegisteredPhoneNumbersModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAInitModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAProvider;
import com.gigya.android.sdk.interruption.tfa.models.TFAProvidersModel;
import com.gigya.android.sdk.interruption.tfa.models.TFATotpRegisterModel;
import com.gigya.android.sdk.interruption.tfa.models.TFAVerificationCodeModel;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.session.ISessionService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GigyaTFAResolver<A extends GigyaAccount> extends GigyaFinalizeResolver<A> {

    private static final String LOG_TAG = "GigyaTFAResolver";

    public GigyaTFAResolver(Config config,
                            ISessionService sessionService,
                            IBusinessApiService<A> businessApiService,
                            GigyaApiResponse originalResponse,
                            GigyaLoginCallback<A> loginCallback) {
        super(config, sessionService, businessApiService, originalResponse, loginCallback);
        requestTFAProviders();
    }

    // Dynamic fields for initialization.
    private List<TFAProvider> activeProviders;

    public List<TFAProvider> getActiveProviders() {
        return activeProviders;
    }

    private List<TFAProvider> inactiveProviders;

    public List<TFAProvider> getInactiveProviders() {
        return inactiveProviders;
    }

    // Dynamic fields.
    private String _gigyaAssertion;
    private String _phvToken;
    private String _sctToken;

    @Override
    public void clear() {
        GigyaLogger.debug(LOG_TAG, "clear: Nullity data");
        _phvToken = null;
        _gigyaAssertion = null;
        if (this.inactiveProviders != null) {
            this.inactiveProviders.clear();
        }
        if (this.activeProviders != null) {
            this.activeProviders.clear();
        }
        _sctToken = null;
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
    private void requestTFAProviders() {
        GigyaLogger.debug(LOG_TAG, "Resolver init - request providers");
        // Request providers. Populate active & inactive on response.
        _businessApiService.getTFAProviders(_regToken, new GigyaCallback<TFAProvidersModel>() {
            @Override
            public void onSuccess(TFAProvidersModel model) {
                GigyaTFAResolver.this.activeProviders = model.getActiveProviders();
                GigyaTFAResolver.this.inactiveProviders = model.getInactiveProviders();
                forwardInitialInterruption();
            }

            @Override
            public void onError(GigyaError error) {
                forwardError(error);
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
        _businessApiService.initTFA(_regToken, provider, mode, new GigyaCallback<TFAInitModel>() {
            @Override
            public void onSuccess(TFAInitModel model) {
                _gigyaAssertion = model.getGigyaAssertion();
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
     * Finalize the TFA flow once a provider assertion is available.
     *
     * @param providerAssertion Gigya TFA provider assertion token.
     */
    private void finalizeTFA(String providerAssertion) {
        GigyaLogger.debug(LOG_TAG, "finalizeTFA: finalize with pa = " + providerAssertion);
        // Generate & send request
        _businessApiService.finalizeTFA(_regToken, _gigyaAssertion, providerAssertion, new GigyaCallback<GigyaApiResponse>() {
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

    //endregion

    //region TOTP

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
        initTFA(GigyaDefinitions.TFA.TOTP, "verify", arguments);
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
        _businessApiService.registerTotp(_gigyaAssertion, new GigyaCallback<TFATotpRegisterModel>() {
            @Override
            public void onSuccess(TFATotpRegisterModel model) {
                _sctToken = model.getSctToken();
                if (isAttached()) {
                    _loginCallback.get().onTotpTFAQrCodeAvailable(model.getQrCode());
                }
            }

            @Override
            public void onError(GigyaError error) {
                forwardError(error);
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
        _businessApiService.verifyTotp(code, _gigyaAssertion, _sctToken, new GigyaCallback<TFACompleteVerificationModel>() {
            @Override
            public void onSuccess(TFACompleteVerificationModel model) {
                finalizeTFA(model.getProviderAssertion());
            }

            @Override
            public void onError(GigyaError error) {
                forwardError(error);
            }
        });
    }

    //endregion

    //region PHONE

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
        _businessApiService.getRegisteredPhoneNumbers(_gigyaAssertion, new GigyaCallback<TFAGetRegisteredPhoneNumbersModel>() {
            @Override
            public void onSuccess(TFAGetRegisteredPhoneNumbersModel model) {
                if (isAttached()) {
                    _loginCallback.get().onRegisteredTFAPhoneNumbers(model.getPhones());
                }
            }

            @Override
            public void onError(GigyaError error) {
                forwardError(error);
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
        GigyaLogger.debug(LOG_TAG, "sendPhoneVerificationCode: method = " + method + " isVerify = " + isVerify);
        // Generate & send request
        final GigyaCallback<TFAVerificationCodeModel> callback = new GigyaCallback<TFAVerificationCodeModel>() {
            @Override
            public void onSuccess(TFAVerificationCodeModel model) {
                _phvToken = model.getPhvToken();
                if (isAttached()) {
                    _loginCallback.get().onPhoneTFAVerificationCodeSent();
                }
            }

            @Override
            public void onError(GigyaError error) {
                forwardError(error);
            }
        };
        if (isVerify) {
            _businessApiService.verifyPhoneNumber(_gigyaAssertion, numberOrId, method, callback);
        } else {
            _businessApiService.registerPhoneNumber(_gigyaAssertion, numberOrId, method, callback);
        }
    }

    /**
     * Submit received code (via sms or voice) in order to finalize the login.
     *
     * @param code Authentication code.
     */
    protected void submitPhoneCode(String code) {
        GigyaLogger.debug(LOG_TAG, "submitPhoneCode: with code = " + code);
        _businessApiService.completePhoneVerification(_gigyaAssertion, code, _phvToken, new GigyaCallback<TFACompleteVerificationModel>() {
            @Override
            public void onSuccess(TFACompleteVerificationModel model) {
                finalizeTFA(model.getProviderAssertion());
            }

            @Override
            public void onError(GigyaError error) {
                forwardError(error);
            }
        });
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
        _businessApiService.getRegisteredEmails(_gigyaAssertion, new GigyaCallback<TFAGetEmailsModel>() {
            @Override
            public void onSuccess(TFAGetEmailsModel model) {
                final List<TFAEmail> emails = model.getEmails();
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
    protected void verifyWithEmail(TFAEmail tfaEmail) {
        GigyaLogger.debug(LOG_TAG, "verifyWithEmail: " + tfaEmail.getObfuscated() + " with id = " + tfaEmail.getId());
        // Generate & send request
        _businessApiService.verifyEmail(tfaEmail.getId(), _gigyaAssertion, new GigyaCallback<TFAVerificationCodeModel>() {
            @Override
            public void onSuccess(TFAVerificationCodeModel model) {
                _phvToken = model.getPhvToken();
                if (isAttached()) {
                    _loginCallback.get().onEmailTFAVerificationEmailSent();
                }
            }

            @Override
            public void onError(GigyaError error) {
                forwardError(error);
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
        _businessApiService.completeEmailVerification(_gigyaAssertion, code, _phvToken, new GigyaCallback<TFACompleteVerificationModel>() {
            @Override
            public void onSuccess(TFACompleteVerificationModel model) {
                finalizeTFA(model.getProviderAssertion());
            }

            @Override
            public void onError(GigyaError error) {
                forwardError(error);
            }
        });
    }

    //endregion

}
