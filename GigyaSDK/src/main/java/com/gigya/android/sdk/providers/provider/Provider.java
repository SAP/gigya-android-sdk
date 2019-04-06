package com.gigya.android.sdk.providers.provider;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.model.account.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.providers.IProviderPermissionsCallback;
import com.gigya.android.sdk.providers.IProviderTokenTrackerListener;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.services.IAccountService;
import com.gigya.android.sdk.services.IApiService;
import com.gigya.android.sdk.services.ISessionService;

import java.util.HashMap;
import java.util.Map;

public abstract class Provider implements IProvider {

    private static final String LOG_TAG = "Provider";

    final protected Config _config;
    final protected ISessionService _sessionService;
    private final IAccountService _accountService;
    final protected IApiService _apiService;
    final private IPersistenceService _psService;
    final private GigyaLoginCallback _gigyaLoginCallback;

    // Dynamic
    protected String _loginMode;
    private String _regToken;
    IProviderTokenTrackerListener _tokenTrackingListener;

    public Provider(Config config, ISessionService sessionService, IAccountService accountService,
                    IApiService apiService, IPersistenceService persistenceService, GigyaLoginCallback gigyaLoginCallback) {
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _apiService = apiService;
        _psService = persistenceService;
        _gigyaLoginCallback = gigyaLoginCallback;

        if (supportsTokenTracking()) {
            _tokenTrackingListener = new IProviderTokenTrackerListener() {
                @Override
                public void onTokenChange(String provider, String providerSession, IProviderPermissionsCallback permissionsCallback) {
                    GigyaLogger.debug(LOG_TAG, getName() + ": onProviderTrackingTokenChanges: provider = "
                            + provider + ", providerSession =" + providerSession);

                    // TODO: 03/04/2019 refreshNativeProviderSessions.
                    _apiService.refreshNativeProviderSession(providerSession, permissionsCallback);
                }
            };
        }
    }

    @Override
    public void onCanceled() {
        _gigyaLoginCallback.onOperationCanceled();
    }

    @Override
    public void onLoginSuccess(String providerSessions, String loginMode) {
        GigyaLogger.debug(LOG_TAG, "onProviderLoginSuccess: provider = "
                + getName() + ", providerSessions = " + providerSessions);

        // Call intermediate load to give the client the option to trigger his own progress indicator.
        _gigyaLoginCallback.onIntermediateLoad();

        final Map<String, Object> params = new HashMap<>();
        params.put("providerSessions", providerSessions);
        params.put("loginMode", loginMode);
        if (loginMode.equals("link") && _regToken != null) {
            params.put("regToken", _regToken);
        }
        _apiService.nativeSocialLogin(params, _gigyaLoginCallback, new Runnable() {
            @Override
            public void run() {
                // Persist used social provider.
                _psService.addSocialProvider(getName());
                if (supportsTokenTracking()) {
                    trackTokenChange();
                }
            }
        });
    }

    @Override
    public void onLoginFailed(String error) {
        GigyaLogger.debug(LOG_TAG, "onProviderLoginFailed: provider = "
                + getName() + ", error =" + error);
        _gigyaLoginCallback.onError(GigyaError.errorFrom(error));
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
        _apiService.getAccount(_gigyaLoginCallback);
    }

    @Override
    public void setRegToken(String regToken) {
        _regToken = regToken;
    }
}
