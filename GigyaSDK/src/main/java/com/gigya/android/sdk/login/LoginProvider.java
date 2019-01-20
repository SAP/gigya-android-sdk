package com.gigya.android.sdk.login;


import android.content.Context;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.SessionInfo;

import java.util.List;
import java.util.Map;

public abstract class LoginProvider {

    public abstract String getName();

    public static class Errors {
        public static final String USER_CANCELLED = "user_cancelled";
        public static final String AUTHENTICATION_DENIED = "authentication_denied";
    }

    protected String providerClientId;
    protected final LoginProviderCallbacks loginCallbacks;
    protected final LoginProviderTrackerCallback trackerCallback;

    public LoginProvider(LoginProviderCallbacks loginCallbacks, LoginProviderTrackerCallback trackerCallback) {
        this.loginCallbacks = loginCallbacks;
        this.trackerCallback = trackerCallback;
    }

    public abstract void login(Context context, Map<String, Object> loginParams);

    public abstract void logout(Context context);

    public abstract String getProviderSessionsForRequest(String tokenOrCode, long expiration, String uid);

    public void updateProviderClientId(String providerClientId) {
        this.providerClientId = providerClientId;
    }

    public boolean clientIdRequired() {
        return false;
    }

    //region Track token changes

    // TODO: 15/01/2019 Don't need session manager.
    public void trackTokenChanges(@Nullable SessionManager sessionManager) {
        // Stub. Override only if provider tracks token changes.
    }

    //endregion

    //region Interfacing

    public interface LoginProviderConfigCallback {
        void onConfigurationRequired(LoginProvider provider);
    }

    public interface LoginProviderCallbacks {

        void onCanceled();

        void onProviderLoginSuccess(LoginProvider provider, String providerSessions);

        void onProviderLoginFailed(String provider, String error);

        void onProviderSession(LoginProvider provider, SessionInfo sessionInfo);
    }

    public interface LoginProviderTrackerCallback {

        void onProviderTrackingTokenChanges(String provider, String providerSession, LoginProvider.LoginPermissionCallbacks permissionCallbacks);
    }

    public interface LoginPermissionCallbacks {

        void granted();

        void noAccess();

        void cancelled();

        void declined(List<String> declined);

        void failed(String error);
    }

    //endregion

}
