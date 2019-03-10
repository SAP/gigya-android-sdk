package com.gigya.android.sdk.providers;


import android.content.Context;
import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaContext;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.services.AccountService;
import com.gigya.android.sdk.services.ApiService;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.services.PersistenceService;
import com.gigya.android.sdk.services.SessionService;

import java.util.List;
import java.util.Map;

public abstract class LoginProvider {

    private static final String LOG_TAG = "LoginProvider";

    protected GigyaLoginCallback _callback;
    protected String providerClientId;

    public abstract String getName();

    protected Config _config;
    private ApiService _apiService;
    private PersistenceService _persistenceService;
    private SessionService _sessionService;
    private AccountService _accountService;

    public LoginProvider(GigyaContext gigyaContext, GigyaLoginCallback callback) {
        _config = gigyaContext.getConfig();
        _apiService = gigyaContext.getApiService();
        _persistenceService = gigyaContext.getPersistenceService();
        _sessionService = gigyaContext.getSessionService();
        _sessionService = gigyaContext.getSessionService();
        _callback = callback;
    }

    //region Callbacks

    /*
     Determine if we need to fetch SDK configuration.
     */
    protected LoginProvider.LoginProviderConfigCallback _configCallback = new LoginProvider.LoginProviderConfigCallback() {
        @Override
        public void onConfigurationRequired(final Context appContext, final LoginProvider provider, final Map<String, Object> params) {
            _apiService.loadConfig(new Runnable() {
                @Override
                public void run() {
                    if (_config.isProviderSynced()) {
                        /* Update provider client id if available */
                        final String providerClientId = _config.getAppIds().get(provider.getName());
                        if (providerClientId != null) {
                            provider.updateProviderClientId(providerClientId);
                        }
                        provider.login(appContext, params);
                    }
                }
            });
        }
    };

    public void configurationRequired(final Context context, final Map<String, Object> params) {
        _configCallback.onConfigurationRequired(context, this, params);
    }

    /*
    Token tracker callback. Shared between all providers (if needed).
    */
    protected LoginProvider.LoginProviderTrackerCallback _loginTrackerCallback = new LoginProvider.LoginProviderTrackerCallback() {
        @Override
        public void onProviderTrackingTokenChanges(String provider, String providerSession, final LoginProvider.LoginPermissionCallbacks permissionCallbacks) {
            GigyaLogger.debug(LOG_TAG, "onProviderTrackingTokenChanges: provider = "
                    + provider + ", providerSession =" + providerSession);
            // Refresh session token.
            _apiService.refreshNativeProviderSession(providerSession, permissionCallbacks);
        }
    };

    /* Login provider operation callbacks. */
    protected LoginProvider.LoginProviderCallbacks _loginCallbacks = new LoginProvider.LoginProviderCallbacks() {

        @Override
        public void onCanceled() {
            _callback.onOperationCanceled();
        }

        @Override
        public void onProviderLoginSuccess(final LoginProvider provider, String providerSessions) {
            GigyaLogger.debug(LOG_TAG, "onProviderLoginSuccess: provider = "
                    + provider + ", providerSessions = " + providerSessions);
            // Call intermediate load to give the client the option to trigger his own progress indicator.
            _callback.onIntermediateLoad();

            // all notifyLogin to submit sign in process.
            _apiService.notifyLogin(providerSessions, _callback, new Runnable() {
                @Override
                public void run() {
                    //Safe to say this is the current selected provider.
                    GigyaLogger.debug(LOG_TAG, "onProviderLoginSuccess: notifyLogin completion handler");
                    _persistenceService.addSocialProvider(provider.getName());
                    provider.trackTokenChanges(_sessionService);
                }
            });
        }

        /* Login process via Web has generated a new session. */
        @Override
        public void onProviderSession(final LoginProvider provider, SessionInfo sessionInfo) {
            // Call intermediate load to give the client the option to trigger his own progress indicator */
            _callback.onIntermediateLoad();

            _persistenceService.addSocialProvider(provider.getName());

            // Update session and call getAccountInfo.
            _sessionService.setSession(sessionInfo);
            _accountService.invalidateAccount();
            _apiService.getAccount(_callback);
        }

        @Override
        public void onProviderLoginFailed(String provider, String error) {
            GigyaLogger.debug(LOG_TAG, "onProviderLoginFailed: provider = "
                    + provider + ", error =" + error);
            _callback.onError(GigyaError.errorFrom(error));
        }
    };

    //endregion

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

    public void trackTokenChanges(@NonNull SessionService sessionService) {
        // Stub. Override only if provider tracks token changes.
    }

    //endregion

    //region Interfacing

    public interface LoginProviderConfigCallback {
        void onConfigurationRequired(Context appContext, LoginProvider provider, Map<String, Object> params);
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

    public static class Errors {
        public static final String USER_CANCELLED = "user_cancelled";
        public static final String AUTHENTICATION_DENIED = "authentication_denied";
    }
}
