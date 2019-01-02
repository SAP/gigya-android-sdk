package com.gigya.android.sdk.network;

import android.support.annotation.NonNull;

public class GigyaError {

    public static class Codes {

        public static final int ERROR_ACCOUNT_PENDING_REGISTRATION = 206001;
        public static final int ERROR_ACCOUNT_PENDING_VERIFICATION = 206002;
        public static final int ERROR_PERMISSION_DENIED = 403007;
        public static final int ERROR_PENDING_PASSWORD_CHANGE = 403100;
        public static final int ERROR_PENDING_TFA_VERIFICATION = 403101;
        public static final int ERROR_PENDING_TFA_REGISTRATION = 403102;
        public static final int ERROR_IDENTITY_EXISTS = 409001;
        public static final int ERORR_SOCIAL_PROVIDER_EXISTS = 409002;

    }

    private int errorCode;
    private String localizedMessage;
    private String callId;

    public GigyaError(int errorCode, String localizedMessage, String callId) {
        this.errorCode = errorCode;
        this.localizedMessage = localizedMessage;
        this.callId = callId;
    }

    public static GigyaError generalError() {
        return new GigyaError(400, "", "");
    }

    @NonNull
    @Override
    public String toString() {
        return "<< Gigya error: code: " +
                this.errorCode +
                ", message: " +
                this.localizedMessage +
                ", callId: " +
                this.callId;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getLocalizedMessage() {
        return localizedMessage;
    }

    public String getCallId() {
        return callId;
    }

}
