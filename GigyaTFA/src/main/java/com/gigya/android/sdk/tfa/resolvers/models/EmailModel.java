package com.gigya.android.sdk.tfa.resolvers.models;

public class EmailModel {

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
