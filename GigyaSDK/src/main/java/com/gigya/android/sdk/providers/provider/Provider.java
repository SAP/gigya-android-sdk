package com.gigya.android.sdk.providers.provider;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.IApiObservable;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.providers.IProviderPermissionsCallback;
import com.gigya.android.sdk.providers.IProviderTokenTrackerListener;
import com.gigya.android.sdk.session.ISessionService;

import java.util.HashMap;
import java.util.Map;

public abstract class Provider implements IProvider {

    private static final String LOG_TAG = "Provider";

    final protected Config _config;
    final protected ISessionService _sessionService;
    private final IAccountService _accountService;
    final private IPersistenceService _psService;
    final private GigyaLoginCallback _gigyaLoginCallback;
    final private IApiObservable _observable;

    // Dynamic
    protected String _loginMode;
    private String _regToken;
    IProviderTokenTrackerListener _tokenTrackingListener;

    public Provider(Config config, ISessionService sessionService, IAccountService accountService, IPersistenceService persistenceService,
                    IApiObservable observable, GigyaLoginCallback gigyaLoginCallback) {
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _psService = persistenceService;
        _observable = observable;
        _gigyaLoginCallback = gigyaLoginCallback;

        if (supportsTokenTracking()) {
            _tokenTrackingListener = new IProviderTokenTrackerListener() {
                @Override
                public void onTokenChange(String provider, String providerSession, final IProviderPermissionsCallback permissionsCallback) {
                    GigyaLogger.debug(LOG_TAG, getName() + ": onProviderTrackingTokenChanges: provider = "
                            + provider + ", providerSession =" + providerSession);
                    // Setup refresh session request.
                    final Map<String, Object> params = new HashMap<>();
                    params.put("providerSession", providerSession);
                    // Notify.
                    _observable.send(
                            GigyaDefinitions.API.API_REFRESH_PROVIDER_SESSION,
                            params,
                            permissionsCallback
                    );
                }
            };
        }
    }

    @Override
    public void onCanceled() {
        _gigyaLoginCallback.onOperationCanceled();
        _observable.dispose();
    }

    @Override
    public void onLoginSuccess(String providerSessions, String loginMode) {
        GigyaLogger.debug(LOG_TAG, "onProviderLoginSuccess: provider = "
                + getName() + ", providerSessions = " + providerSessions);
        // Call intermediate load to give the client the option to trigger his own progress indicator.
        _gigyaLoginCallback.onIntermediateLoad();
        // Setup request.
        final Map<String, Object> params = new HashMap<>();
        params.put("providerSessions", providerSessions);
        params.put("loginMode", loginMode);
        if (loginMode.equals("link") && _regToken != null) {
            params.put("regToken", _regToken);
        }
        // Notify.
        _observable.send(
                GigyaDefinitions.API.API_NOTIFY_SOCIAL_LOGIN,
                params,
                _gigyaLoginCallback,
                new Runnable() {
                    @Override
                    public void run() {
                        _psService.addSocialProvider(getName());
                        if (supportsTokenTracking()) {
                            trackTokenChange();
                        }
                    }
                }
        );
    }

    @Override
    public void onLoginFailed(String error) {
        GigyaLogger.debug(LOG_TAG, "onProviderLoginFailed: provider = "
                + getName() + ", error =" + error);
        _gigyaLoginCallback.onError(GigyaError.errorFrom(error));
        _observable.dispose();
    }

    @Override
    public void onProviderSession(SessionInfo sessionInfo) {
        // Call intermediate load to give the client the option to trigger his own progress indicator
        _gigyaLoginCallback.onIntermediateLoad();
        // Persist used social provider.
        _psService.addSocialProvider(getName());
        // Set new session.
        _sessionService.setSession(sessionInfo);
        // Force fetch account.
        _accountService.invalidateAccount();

        _observable.send(
                GigyaDefinitions.API.API_GET_ACCOUNT_INFO,
                null,
                _gigyaLoginCallback
        );
    }

    @Override
    public void setRegToken(String regToken) {
        _regToken = regToken;
    }
}
