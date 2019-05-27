package com.gigya.android.sdk.interruption.tfa.models;

public class TFAEmail {

    private String id;
    private String obfuscated;
    private String lastVerification;

    public String getId() {
        return id;
    }

    public String getObfuscated() {
        return obfuscated;
    }

    public String getLastVerification() {
        return lastVerification;
    }
}
