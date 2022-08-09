package com.gigya.android.sdk.auth.models;

public class WebAuthnOptionsBinding {

    public WebAuthnOptionsBinding(String token, int requestCode, String rp) {
        this.token = token;
        this.requestCode = requestCode;
        this.rp = rp;
    }

    public WebAuthnOptionsBinding(int requestCode, String rp, String uid) {
        this.requestCode = requestCode;
        this.rp = rp;
        this.uid = uid;
    }

    public String token;
    public int requestCode;
    public String rp;

    public String uid;
}
