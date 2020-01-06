package com.gigya.android.sdk.network;

/**
 * Gigya main REST model.
 */
public abstract class GigyaResponseModel {

    protected int statusCode;
    protected int errorCode;
    protected String callId;

    public int getStatusCode() {
        return statusCode;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getCallId() {
        return callId;
    }
}
