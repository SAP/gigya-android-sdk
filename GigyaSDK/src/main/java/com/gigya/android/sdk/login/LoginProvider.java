package com.gigya.android.sdk.login;

public abstract class LoginProvider {

    public LoginProvider getProvider(String provider) {
        return null;
    }

    public abstract void login();

}
