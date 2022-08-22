package com.gigya.android.sdk.auth.models;

public class WebAuthnOptionsBinding {

    public WebAuthnOptionsBinding(String token,
                                  int requestCode,
                                  String type,
                                  WebAuthnUserModel userModel) {
        this.token = token;
        this.requestCode = requestCode;
        this.type = type;
        this.userModel = userModel;
    }

    public WebAuthnOptionsBinding(String token, int requestCode) {
        this.token = token;
        this.requestCode = requestCode;
    }

    public String token;
    public int requestCode;
    public WebAuthnUserModel userModel;
    public String type; // cross-platform or platform.

}
