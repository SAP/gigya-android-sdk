package com.gigya.android.sdk.api;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.interruption.LinkedAccountResolver;
import com.gigya.android.sdk.interruption.LoginIdentifierExistsResolver;
import com.gigya.android.sdk.interruption.TFAResolver;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;

abstract class BaseLoginApi<T> extends BaseApi<T> {

    BaseLoginApi(@Nullable Class<T> clazz) {
        super(clazz);
    }

    void evaluateError(GigyaResponse response, final GigyaLoginCallback loginCallback) {
        if (!apiInterrupted(response, loginCallback)) {
            /* Interruption is not handled. Forward the error. */
            loginCallback.forwardError(response);
        }
    }

    /* Handle specific interruptions according to pre-defined handled error codes. */
    private boolean apiInterrupted(GigyaResponse response, final GigyaLoginCallback loginCallback) {
        if (configuration.isInterruptionsEnabled()) {
            /* Get regToken from parameter map. */
            final String regToken = response.getField("regToken", String.class);
            final int errorCode = response.getErrorCode();
            switch (errorCode) {
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION:
                    loginCallback.onPendingVerification(response, regToken);
                    return true;
                case GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION:
                    loginCallback.onPendingRegistration(response, regToken);
                    return true;
                case GigyaError.Codes.ERROR_PENDING_PASSWORD_CHANGE:
                    loginCallback.onPendingPasswordChange(response);
                    return true;
                case GigyaError.Codes.ERROR_LOGIN_IDENTIFIER_EXISTS:
                    new LoginIdentifierExistsResolver(response, loginCallback).resolve(regToken);
                    return true;
                case GigyaError.Codes.ERROR_PENDING_TWO_FACTOR_REGISTRATION:
                    new TFAResolver(loginCallback).regToken(regToken).getProviders();
                    return true;
            }
        }
        return false;
    }

    /* Evaluating responses that are tagged as success but still require error handling. */
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
                new LinkedAccountResolver(response, loginCallback).resolve(regToken);
                return true;
        }
        return false;
    }
}
