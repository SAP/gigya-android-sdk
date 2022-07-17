package com.gigya.android.sdk.auth.models;

public class WebAuthnOptionsToken {

    public WebAuthnOptionsToken(String token, int requestCode) {
        this.token = token;
        this.requestCode = requestCode;
    }

    public String token;
    public int requestCode;
}
