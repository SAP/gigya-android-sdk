package com.gigya.android.sdk.auth.models;

public class WebAuthnOptionsBinding {

    public WebAuthnOptionsBinding(String token, int requestCode, String rp) {
        this.token = token;
        this.requestCode = requestCode;
        this.rp = rp;
    }

    public String token;
    public int requestCode;
    public String rp;
}
