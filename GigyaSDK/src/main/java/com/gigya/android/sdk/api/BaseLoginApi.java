package com.gigya.android.sdk.api;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.utils.ObjectUtils;

import java.util.Map;

abstract class BaseLoginApi<T> extends BaseApi<T> {

    BaseLoginApi(@Nullable Class<T> clazz) {
        super(clazz);
    }

    void evaluateError(GigyaResponse response, Map<String, Object> params, final GigyaLoginCallback callback) {
        if (!apiInterrupted(response, params, callback)) {
            final int errorCode = response.getErrorCode();
            final String localizedMessage = response.getErrorDetails();
            final String callId = response.getCallId();
            callback.onError(new GigyaError(response.asJson(), errorCode, localizedMessage, callId));
        }
    }

    private boolean apiInterrupted(GigyaResponse response, Map<String, Object> params, final GigyaLoginCallback callback) {
        /* Get regToken from parameter map. */
        final String regToken = ObjectUtils.firstNonNull((String) params.get("regToken"), "");
        final int errorCode = response.getErrorCode();
        switch (errorCode) {
            case GigyaError.Codes.ERROR_ACCOUNT_PENDING_VERIFICATION:
                callback.onPendingVerification(regToken);
                return true;
            case GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION:
                callback.onPendingRegistration(regToken);
                return true;
            case GigyaError.Codes.ERROR_PENDING_PASSWORD_CHANGE:
                callback.onPendingPasswordChange();
                return true;
        }
        return false;
    }
}
