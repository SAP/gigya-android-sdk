package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.IApiObservable;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.interruption.link.GigyaLinkAccountsResolver;
import com.gigya.android.sdk.interruption.tfa.GigyaTFARegistrationResolver;
import com.gigya.android.sdk.interruption.tfa.GigyaTFAVerificationResolver;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import java.util.HashMap;
import java.util.Map;

public class InterruptionHandler implements IInterruptionsHandler {

    public static final String LOG_TAG = "InterruptionHandler";

    //Dependencies
    final private IoCContainer _container;

    private boolean _enabled = true;

    public InterruptionHandler(IoCContainer container) {
        _container = container.clone();
    }

    @Override
    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return _enabled;
    }

    @Override
    public void resolve(GigyaApiResponse apiResponse, IApiObservable observable, GigyaLoginCallback loginCallback) {
        if (_enabled) {
            final int errorCode = apiResponse.getErrorCode();
            GigyaLogger.debug(LOG_TAG, "resolve: with errorCode = " + errorCode);

            final IoCContainer resolverContainer =
                    _container.clone()
                            .bind(IApiObservable.class, observable)
                            .bind(GigyaApiResponse.class, apiResponse)
                            .bind(GigyaLoginCallback.class, loginCallback);

            try {
                switch (errorCode) {
                    case GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION:
                        loginCallback.onPendingVerification(apiResponse, getRegToken(apiResponse));
                        break;
                    case GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION:
                        loginCallback.onPendingRegistration(apiResponse, getRegToken(apiResponse));
                        break;
                    case GigyaError.Codes.ERROR_PENDING_PASSWORD_CHANGE:
                        loginCallback.onPendingPasswordChange(apiResponse);
                        break;
                    case GigyaError.Codes.ERROR_LOGIN_IDENTIFIER_EXISTS:
                        GigyaLinkAccountsResolver linkAccountsResolver =
                                resolverContainer.createInstance(GigyaLinkAccountsResolver.class);
                        linkAccountsResolver.start();
                        break;
                    case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION:
                        GigyaTFARegistrationResolver registrationResolver =
                                resolverContainer.createInstance(GigyaTFARegistrationResolver.class);
                        registrationResolver.start();
                        break;
                    case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION:
                        GigyaTFAVerificationResolver verificationResolver =
                                resolverContainer.createInstance(GigyaTFAVerificationResolver.class);
                        verificationResolver.start();
                        break;
                    case GigyaError.Codes.SUCCESS_ERROR_ACCOUNT_LINKED:
                        finalizeRegistration(apiResponse, observable, loginCallback);
                        break;
                    default:
                        // Unsupported error
                        loginCallback.onError(GigyaError.fromResponse(apiResponse));
                        break;
                }
            } catch (Exception e) {
                // error with creating resolvers - could be missing container dependencies
                GigyaLogger.error(LOG_TAG, e.getMessage());

                loginCallback.onError(GigyaError.fromResponse(apiResponse));
            }
        }
    }

    private String getRegToken(GigyaApiResponse apiResponse) {
        return apiResponse.getField("regToken", String.class);
    }

    private void finalizeRegistration(GigyaApiResponse apiResponse, IApiObservable observable, GigyaLoginCallback loginCallback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", getRegToken(apiResponse));
        params.put("include", "profile,data,emails,subscriptions,preferences");
        params.put("includeUserInfo", "true");
        // Api.
        final String api = GigyaDefinitions.API.API_FINALIZE_REGISTRATION;
        // Notify observer.
        observable.send(api, params, loginCallback);
    }


    // TODO: #baryo remove?
//    // Supported interruptions.
//    private final List<Integer> _interruptionList = Arrays.asList(
//            GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION,
//            GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION,
//            GigyaError.Codes.ERROR_LOGIN_IDENTIFIER_EXISTS,
//            GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION,
//            GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION,
//            GigyaError.Codes.SUCCESS_ERROR_ACCOUNT_LINKED);
//
//    public boolean isSupportedInterruption(int errorCode) {
//        return _interruptionList.contains(errorCode);
//    }
}
