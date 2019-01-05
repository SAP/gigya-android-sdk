package com.gigya.android.sdk.login;


import android.content.Context;

import java.util.List;
import java.util.Map;

public abstract class LoginProvider {

    protected final LoginProviderCallbacks loginCallbacks;

    public LoginProvider(LoginProviderCallbacks loginCallbacks) {
        this.loginCallbacks = loginCallbacks;
    }

    public abstract void login(Context context, Map<String, Object> loginParams);

    public abstract void logout();

    //region Interfacing

    public abstract static class LoginProviderCallbacks {

        public abstract void onProviderLoginSuccess(String provider, String token, long expiration);

        public abstract void onProviderLoginFailed(String provider, String error);
    }

    public abstract static class LoginPermissionCallbacks {

        public abstract void granted();

        public abstract void noAccess();

        public abstract void cancelled();

        public abstract void declined(List<String> declined);

        public abstract void failed(String error);
    }

    //endregion
}
