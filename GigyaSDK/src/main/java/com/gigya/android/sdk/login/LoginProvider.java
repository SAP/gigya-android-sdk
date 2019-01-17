package com.gigya.android.sdk.login;


import android.app.Activity;
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

    public void updateProviderClientId(String providerClientId) {
        this.providerClientId = providerClientId;
    }

    public boolean clientIdRequired() {
        return false;
    }

    public abstract void login(Context context, Map<String, Object> loginParams);

    public abstract void logout(Context context);

    public abstract String getProviderSessionsForRequest(String tokenOrCode, long expiration, String uid);

    //region Track token changes

    // TODO: 15/01/2019 Don't need session manager.
    public void trackTokenChanges(@Nullable SessionManager sessionManager) {
        // Stub. Override only if provider tracks token changes.
    }

    //endregion

    //region Interfacing

    public interface LoginProviderConfigCallback {
        void onConfigurationRequired();
    }

    public abstract static class LoginProviderCallbacks {

        public abstract void onConfigurationRequired(Activity activity, LoginProvider loginProvider);

        public abstract void onCanceled();

        public abstract void onProviderSelected(LoginProvider provider);

        public abstract void onProviderLoginSuccess(String provider, String providerSessions);

        public abstract void onProviderLoginFailed(String provider, String error);

        public void onProviderSession(String provider, SessionInfo sessionInfo) {
            // Stub.
        }
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
