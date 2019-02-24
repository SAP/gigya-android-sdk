package com.gigya.android.sdk.model;

public abstract class BaseModel {

    protected int errorCode;
    protected String callId;

    public int getErrorCode() {
        return errorCode;
    }

    public String getCallId() {
        return callId;
    }
}
