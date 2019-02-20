package com.gigya.android.sdk.api;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.interruption.LinkedAccountResolver;
import com.gigya.android.sdk.interruption.LoginIdentifierExistsResolver;
import com.gigya.android.sdk.interruption.PendingPasswordChangeResolver;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;

abstract class BaseLoginApi<T> extends BaseApi<T> {

    BaseLoginApi(@Nullable Class<T> clazz) {
        super(clazz);
    }

    void evaluateError(GigyaResponse response, final GigyaLoginCallback loginCallback) {
        if (!apiInterrupted(response, loginCallback)) {
            final int errorCode = response.getErrorCode();
            final String localizedMessage = response.getErrorDetails();
            final String callId = response.getCallId();
            loginCallback.onError(new GigyaError(response.asJson(), errorCode, localizedMessage, callId));
        }
    }

    private boolean apiInterrupted(GigyaResponse response, final GigyaLoginCallback loginCallback) {
        if (configuration.isInterruptionsEnabled()) {
            /* Get regToken from parameter map. */
            final String regToken = response.getField("regToken", String.class);
            final int errorCode = response.getErrorCode();
            switch (errorCode) {
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION:
                    loginCallback.onPendingVerification(regToken);
                    return true;
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION:
                    loginCallback.onPendingRegistration(regToken);
                    return true;
                case GigyaError.Codes.ERROR_PENDING_PASSWORD_CHANGE:
                    loginCallback.onPendingPasswordChange(new PendingPasswordChangeResolver(loginCallback, regToken));
                    return true;
                case GigyaError.Codes.ERROR_LOGIN_IDENTIFIER_EXISTS:
                    new LoginIdentifierExistsResolver(loginCallback).resolve(regToken);
                    return true;
            }
        }
        return false;
    }

    boolean evaluateSuccessError(GigyaResponse response, final GigyaLoginCallback loginCallback) {
        if (!configuration.isInterruptionsEnabled()) {
            return false;
        }
        final int errorCode = response.getErrorCode();
        if (errorCode == 0) {
            return false;
        }
        switch (errorCode) {
            case GigyaError.Codes.SUCCESS_ERROR_ACCOUNT_LINKED:
                final String regToken = response.getField("regToken", String.class);
                new LinkedAccountResolver(loginCallback).resolve(regToken);
                return true;
        }
        return false;
    }
}
