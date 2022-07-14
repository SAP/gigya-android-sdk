package com.gigya.android.sdk.auth.models;

public class WebAuthnAuthenticatorSelectionModel {
    boolean requireResidentKey;
    String userVerification;
    WebAuthnAuthenticatorSelectionType authenticatorAttachment;
}
