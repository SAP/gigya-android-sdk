package com.gigya.android.sdk.auth;

public interface IOauthService {

    void connect(String token);

    void authorize(String token);

    void token(String code);
}
