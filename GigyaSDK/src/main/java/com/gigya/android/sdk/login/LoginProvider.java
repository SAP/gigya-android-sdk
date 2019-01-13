package com.gigya.android.sdk.login;


import android.content.Context;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.SessionManager;

import java.util.List;
import java.util.Map;

public abstract class LoginProvider {

    public static class Errors {
        public static final String USER_CANCELLED = "user_cancelled";
        public static final String AUTHENTICATION_DENIED = "authentication_denied";
    }

    protected final LoginProviderCallbacks loginCallbacks;
    protected final LoginProviderTrackerCallback trackerCallback;

    public LoginProvider(LoginProviderCallbacks loginCallbacks, LoginProviderTrackerCallback trackerCallback) {
        this.loginCallbacks = loginCallbacks;
        this.trackerCallback = trackerCallback;
    }

    public abstract void login(Context context, Map<String, Object> loginParams);

    public abstract void logout(Context context);

    public abstract String getProviderSessions(String tokenOrCode, long expiration, String uid);

    //region Track token changes

    public void trackTokenChanges(@Nullable SessionManager sessionManager) {
        // Stub.
    }

    //endregion

    //region Interfacing

    public abstract static class LoginProviderCallbacks {

        public abstract void onProviderSelected(LoginProvider provider);

        public abstract void onProviderLoginSuccess(String provider, String providerSessions);

        public abstract void onProviderLoginFailed(String provider, String error);
    }

    public abstract static class LoginProviderTrackerCallback {

        public abstract void onProviderTrackingTokenChanges(String provider, String providerSession, LoginProvider.LoginPermissionCallbacks permissionCallbacks);
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
