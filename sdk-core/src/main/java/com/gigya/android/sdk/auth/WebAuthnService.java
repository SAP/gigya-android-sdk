package com.gigya.android.sdk.auth;

public class WebAuthnService implements IWebAuthnService {

    final private IOauthService oauthService;

    public WebAuthnService(
            IOauthService oauthService
    ) {
        this.oauthService = oauthService;
    }


}
