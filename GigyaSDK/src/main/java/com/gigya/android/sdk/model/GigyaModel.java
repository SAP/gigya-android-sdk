package com.gigya.android.sdk.model;

public abstract class GigyaModel {

    protected int statusCode;
    protected int errorCode;
    protected String callId;

    public int getStatusCode() { return statusCode; }

    public int getErrorCode() {
        return errorCode;
    }

    public String getCallId() {
        return callId;
    }
}
