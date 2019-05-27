package com.gigya.android.sdk.interruption.tfa.models;

public class TFARegisteredPhone {

    private String id;
    private String obfuscated;
    private String lastMethod;
    private String lastVerification;

    public String getId() {
        return id;
    }

    public String getObfuscated() {
        return obfuscated;
    }

    public String getLastMethod() {
        return lastMethod;
    }

    public String getLastVerification() {
        return lastVerification;
    }
}
