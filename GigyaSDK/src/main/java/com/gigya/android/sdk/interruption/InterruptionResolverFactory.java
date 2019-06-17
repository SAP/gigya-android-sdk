package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.containers.IoCContainer;
import com.gigya.android.sdk.interruption.link.GigyaLinkAccountsResolver;
import com.gigya.android.sdk.interruption.tfa.GigyaTFARegistrationResolver;
import com.gigya.android.sdk.interruption.tfa.GigyaTFAVerificationResolver;
import com.gigya.android.sdk.network.GigyaError;

public class InterruptionResolverFactory implements IInterruptionResolverFactory {

    public static final String LOG_TAG = "InterruptionResolverFactory";

    //Dependencies
    final private IoCContainer _container;

    private boolean _enabled = true;

    public InterruptionResolverFactory(IoCContainer container) {
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
    public void resolve(GigyaApiResponse apiResponse, GigyaLoginCallback loginCallback) {
        if (!_enabled) {
            loginCallback.onError(GigyaError.fromResponse(apiResponse));
            return;
        }

        final IoCContainer resolverContainer =
                _container.clone()
                        .bind(GigyaApiResponse.class, apiResponse)
                        .bind(GigyaLoginCallback.class, loginCallback);

        final int errorCode = apiResponse.getErrorCode();
        GigyaLogger.debug(LOG_TAG,
                "resolve: with errorCode = " + errorCode);

        try {
            switch (errorCode) {
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION:
                    loginCallback.onPendingVerification(apiResponse, getRegToken(apiResponse));
                    break;
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION:

                    loginCallback.onPendingRegistration(apiResponse, getRegToken(apiResponse));
                    break;
                case GigyaError.Codes.ERROR_PENDING_PASSWORD_CHANGE:
                    // TODO: #baryo resolver?
                    loginCallback.onPendingPasswordChange(apiResponse);
                    break;
                case GigyaError.Codes.ERROR_LOGIN_IDENTIFIER_EXISTS:
                    resolverContainer.createInstance(GigyaLinkAccountsResolver.class);
                    break;
                case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION:
                    resolverContainer.createInstance(GigyaTFARegistrationResolver.class);
                    break;
                case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION:
                    resolverContainer.createInstance(GigyaTFAVerificationResolver.class);
                    break;
                case GigyaError.Codes.SUCCESS_ERROR_ACCOUNT_LINKED:
                    GigyaFinalizeResolver finalizeResolver = resolverContainer.createInstance(GigyaFinalizeResolver.class);
                    finalizeResolver.finalizeRegistration();
                    break;
                default:
                    handleUnsupportedResponse(apiResponse, loginCallback);
                    break;
            }
        } catch (Exception e) {
            // error with creating resolvers - could be missing container dependencies
            GigyaLogger.error(LOG_TAG, e.getMessage());
            handleUnsupportedResponse(apiResponse, loginCallback);
        } finally {
            resolverContainer.dispose();
        }
    }

    private void handleUnsupportedResponse(GigyaApiResponse apiResponse, GigyaLoginCallback loginCallback) {
        loginCallback.onError(GigyaError.fromResponse(apiResponse));
    }

    private String getRegToken(GigyaApiResponse apiResponse) {
        return apiResponse.getField("regToken", String.class);
    }
}
