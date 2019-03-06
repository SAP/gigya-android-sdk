package com.gigya.android.sdk.api.bloc;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.services.ApiService;

public class BlocHandler<A extends GigyaAccount> {

    private static final String LOG_TAG = "BlocHandler";

    private final ApiService<A> _apiService;

    public BlocHandler(ApiService<A> apiService) {
        _apiService = apiService;
    }

    public boolean evaluateInterruptionError(GigyaApiResponse apiResponse, final GigyaLoginCallback<? extends GigyaAccount> loginCallback) {
        if (_apiService.isInterruptionsEnabled()) {
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
                case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION:
                    optionalResolveForTFA(apiResponse, loginCallback);
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    public boolean evaluateInterruptionSuccess(GigyaApiResponse apiResponse) {
        final int errorCode = apiResponse.getErrorCode();
        if (errorCode == 0) {
            return false;
        }
        GigyaLogger.debug(LOG_TAG, "evaluateInterruptionSuccess: True with errorCode = " + errorCode);
        switch (errorCode) {
            case GigyaError.Codes.SUCCESS_ERROR_ACCOUNT_LINKED:
                return true;
            default:
                return false;
        }
    }

    private String getRegToken(GigyaApiResponse apiResponse) {
        return apiResponse.getField("regToken", String.class);
    }

    private void optionalResolveForConflictingAccounts(final GigyaApiResponse apiResponse, final GigyaLoginCallback<? extends GigyaAccount> loginCallback) {
        new GigyaLinkAccountsResolver<>(_apiService, apiResponse, loginCallback).init();
    }

    private void optionalResolveForTFA(final GigyaApiResponse apiResponse, final GigyaLoginCallback<? extends GigyaAccount> loginCallback) {
        new GigyaTFAResolver<>(_apiService, apiResponse, loginCallback).init();
    }
}
