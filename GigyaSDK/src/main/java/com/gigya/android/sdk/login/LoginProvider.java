package com.gigya.android.sdk.login;


import android.content.Context;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.ApiManager;
import com.gigya.android.sdk.DependencyRegistry;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.PersistenceManager;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.log.GigyaLogger;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;

import java.util.List;
import java.util.Map;

public abstract class LoginProvider {

    private static final String LOG_TAG = "LoginProvider";

    protected GigyaLoginCallback _callback;

    private Configuration _configuration;
    private ApiManager _apiManager;
    private PersistenceManager _persistenceManager;
    private SessionManager _sessionManager;
    private AccountManager _accountManager;

    public LoginProvider(GigyaLoginCallback callback) {
        DependencyRegistry.getInstance().inject(this);
        _callback = callback;
    }

    public void inject(Configuration configuration, ApiManager apiManager, PersistenceManager persistenceManager, SessionManager sessionManager, AccountManager accountManager) {
        _configuration = configuration;
        _apiManager = apiManager;
        _persistenceManager = persistenceManager;
        _sessionManager = sessionManager;
        _accountManager = accountManager;
    }

    public abstract String getName();

    protected String providerClientId;

    // region Callbacks

    /* Determine if we need to fetch SDK configuration. */
    protected LoginProvider.LoginProviderConfigCallback _configCallback = new LoginProvider.LoginProviderConfigCallback() {
        @Override
        public void onConfigurationRequired(final Context appContext, final LoginProvider provider, final Map<String, Object> params) {
            _apiManager.loadConfig(new Runnable() {
                @Override
                public void run() {
                    if (_configuration.isSynced()) {
                        /* Update provider client id if available */
                        final String providerClientId = _configuration.getAppIds().get(provider.getName());
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

            /* Refresh session token. */
            _apiManager.updateProviderSessions(providerSession, permissionCallbacks);
        }
    };

    /* Login provider operation callbacks. */
    protected LoginProvider.LoginProviderCallbacks _loginCallbacks = new LoginProvider.LoginProviderCallbacks() {

        @Override
        public void onCanceled() {
            _callback.onOperationCancelled();
        }

        @Override
        public void onProviderLoginSuccess(final LoginProvider provider, String providerSessions) {
            GigyaLogger.debug(LOG_TAG, "onProviderLoginSuccess: provider = "
                    + provider + ", providerSessions = " + providerSessions);
            /* Call intermediate load to give the client the option to trigger his own progress indicator */
            _callback.onIntermediateLoad();

            _apiManager.notifyLogin(providerSessions, _callback, new Runnable() {
                @Override
                public void run() {
                    /* Safe to say this is the current selected provider. */
                    _accountManager.updateLoginProvider(provider);
                    provider.trackTokenChanges(_sessionManager);

                    _persistenceManager.onLoginProviderUpdated(provider.getName());
                }
            });
        }

        @Override
        public void onProviderSession(final LoginProvider provider, SessionInfo sessionInfo) {
            /* Login process via Web has generated a new session. */

            /* Call intermediate load to give the client the option to trigger his own progress indicator */
            _callback.onIntermediateLoad();

            /* Call notifyLogin to complete sign in process.*/
            _apiManager.notifyLogin(sessionInfo, _callback, new Runnable() {
                @Override
                public void run() {
                    _persistenceManager.onLoginProviderUpdated(provider.getName());
                }
            });
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

    // TODO: 15/01/2019 Don't need session manager.
    public void trackTokenChanges(@Nullable SessionManager sessionManager) {
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
