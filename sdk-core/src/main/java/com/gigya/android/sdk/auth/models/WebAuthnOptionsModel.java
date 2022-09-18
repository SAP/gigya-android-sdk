package com.gigya.android.sdk.auth.models;

public class WebAuthnOptionsModel {

    public String challenge;
    public WebAuthnRpModel rp;
    public WebAuthnUserModel user;
    public WebAuthnAuthenticatorSelectionModel authenticatorSelection;
}
