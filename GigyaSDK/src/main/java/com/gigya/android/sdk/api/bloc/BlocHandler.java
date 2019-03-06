package com.gigya.android.sdk.api.bloc;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.services.AccountService;
import com.gigya.android.sdk.services.ApiService;
import com.gigya.android.sdk.services.SessionService;

public class BlocHandler {

    private static final String LOG_TAG = "BlocHandler";

    final private SessionService _sessionService;
    private AccountService _accountService;
    private ApiService _apiService;

    public BlocHandler(SessionService sessionService, AccountService accountService, ApiService apiService) {
        _sessionService = sessionService;
        _accountService = accountService;
        _apiService = apiService;
    }

    public boolean evaluateInterruptionError(GigyaApiResponse apiResponse, final GigyaLoginCallback loginCallback) {
        if (_sessionService.getConfig().isInterruptionsEnabled()) {
            final int errorCode = apiResponse.getErrorCode();
            GigyaLogger.debug(LOG_TAG, "evaluateInterruptionError: True with errorCode = " + errorCode);
            switch (errorCode) {
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION:
                    return true;
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION:
                    return true;
                case GigyaError.Codes.ERROR_PENDING_PASSWORD_CHANGE:
                    return true;
                case GigyaError.Codes.ERROR_LOGIN_IDENTIFIER_EXISTS:
                    return true;
                case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION:
                case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_VERIFICATION:
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
}
