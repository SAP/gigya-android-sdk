package com.gigya.android.sdk.api.interruption;

import android.support.v4.util.ArrayMap;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.api.interruption.tfa.GigyaTFARegistrationResolver;
import com.gigya.android.sdk.api.interruption.tfa.GigyaTFAVerificationResolver;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;

import java.util.Map;

public class InterruptionsResolver implements IInterruptionsResolver {

    public static final String LOG_TAG = "InterruptionsResolver";

    private ArrayMap<String, GigyaResolver> _resolvers = new ArrayMap<>();

    private boolean _enabled = true;

    @Override
    public void setEnabled(boolean enabled) {
        _enabled = enabled;
    }

    @Override
    public void clearAll() {
        GigyaLogger.debug(LOG_TAG, "clearAll: ");
        for (Map.Entry<String, GigyaResolver> entry : _resolvers.entrySet()) {
            GigyaResolver resolver = entry.getValue();
            resolver.clear();
        }
    }

    @Override
    public boolean evaluateInterruptionError(GigyaApiResponse apiResponse, GigyaLoginCallback loginCallback) {
        if (_enabled) {
            final int errorCode = apiResponse.getErrorCode();
            GigyaLogger.debug(LOG_TAG, "evaluateInterruptionError: True with errorCode = " + errorCode);
            switch (errorCode) {
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION:
                    loginCallback.onPendingRegistration(apiResponse, getRegToken(apiResponse));
                    return true;
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION:
                    loginCallback.onPendingVerification(apiResponse, getRegToken(apiResponse));
                    return true;
                case GigyaError.Codes.ERROR_PENDING_PASSWORD_CHANGE:
                    loginCallback.onPendingPasswordChange(apiResponse);
                    return true;
                case GigyaError.Codes.ERROR_LOGIN_IDENTIFIER_EXISTS:
                    optionalResolveForConflictingAccounts(apiResponse, loginCallback);
                    return true;
                case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION:
                    optionalResolveForTFARegistration(apiResponse, loginCallback);
                    return true;
                case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION:
                    optionalResolveForTFAVerification(apiResponse, loginCallback);
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    @Override
    public boolean evaluateInterruptionSuccess(GigyaApiResponse apiResponse) {
        if (_enabled) {
            final int errorCode = apiResponse.getErrorCode();
            if (errorCode == 0) {
                return false;
            }
            GigyaLogger.debug(LOG_TAG, "evaluateInterruptionSuccess: True with errorCode = " + errorCode);
            switch (errorCode) {
                case GigyaError.Codes.SUCCESS_ERROR_ACCOUNT_LINKED:
                    final String regToken = getRegToken(apiResponse);
                    final GigyaLinkAccountsResolver linkAccountsResolver = (GigyaLinkAccountsResolver) _resolvers.get(GigyaResolver.LINK_ACCOUNTS);
                    if (linkAccountsResolver != null) {
                        linkAccountsResolver.finalizeFlow(regToken);
                    }
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    private String getRegToken(GigyaApiResponse apiResponse) {
        return apiResponse.getField("regToken", String.class);
    }

    /**
     * Initialize conflicting accounts resolver.
     *
     * @param apiResponse   Original error response.
     * @param loginCallback Login result callback.
     */
    private void optionalResolveForConflictingAccounts(final GigyaApiResponse apiResponse, final GigyaLoginCallback loginCallback) {
        GigyaResolver resolver = _resolvers.get(GigyaResolver.LINK_ACCOUNTS);
        if (resolver == null) {
            resolver = new GigyaLinkAccountsResolver<>();
            _resolvers.put(GigyaResolver.LINK_ACCOUNTS, resolver);
        }
        resolver.clear();
        //resolver.init(_apiService, apiResponse, loginCallback);
    }

    /**
     * Initialize TFA registration resolver.
     *
     * @param apiResponse   Original error response.
     * @param loginCallback Login result callback.
     */
    private void optionalResolveForTFARegistration(final GigyaApiResponse apiResponse, final GigyaLoginCallback loginCallback) {
        GigyaResolver resolver = _resolvers.get(GigyaResolver.TFA_REG);
        if (resolver == null) {
            resolver = new GigyaTFARegistrationResolver<>();
            _resolvers.put(GigyaResolver.TFA_REG, resolver);
        }
        resolver.clear();
        //resolver.init(_apiService, apiResponse, loginCallback);
    }

    /**
     * Initialize TFA verification resolver.
     *
     * @param apiResponse   Original error response.
     * @param loginCallback Login result callback.
     */
    private void optionalResolveForTFAVerification(final GigyaApiResponse apiResponse, final GigyaLoginCallback loginCallback) {
        GigyaResolver resolver = _resolvers.get(GigyaResolver.TFA_VER);
        if (resolver == null) {
            resolver = new GigyaTFAVerificationResolver<>();
            _resolvers.put(GigyaResolver.TFA_VER, resolver);
        }
        resolver.clear();
        //resolver.init(_apiService, apiResponse, loginCallback);
    }
}
