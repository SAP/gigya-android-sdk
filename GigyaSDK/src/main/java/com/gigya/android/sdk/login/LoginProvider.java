package com.gigya.android.sdk.login;


import android.content.Context;

import java.util.Map;

public abstract class LoginProvider {

    protected final LoginProviderCallbacks loginCallbacks;

    public LoginProvider(LoginProviderCallbacks loginCallbacks) {
        this.loginCallbacks = loginCallbacks;
    }

    public abstract void login(Context context, Map<String, Object> loginParams);

    public abstract void logout();

    public interface LoginProviderCallbacks {

        void onSuccess(String token, long expiration);

        void onError(String error);
    }
}
