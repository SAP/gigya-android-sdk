package com.gigya.android.sdk.providers.provider;

import android.content.Context;

import com.gigya.android.sdk.Config;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.account.IAccountService;
import com.gigya.android.sdk.api.BusinessApiService;
import com.gigya.android.sdk.api.IApiObservable;
import com.gigya.android.sdk.api.IBusinessApiService;
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

    final protected Context _context;
    final protected Config _config;
    final protected ISessionService _sessionService;
    private final IAccountService _accountService;
    final private IPersistenceService _psService;
    private final GigyaLoginCallback _gigyaLoginCallback;
    private final IBusinessApiService _businessApiService;

    protected boolean _connecting = false;

    // Dynamic
    protected String _loginMode;
    private String _regToken;
    IProviderTokenTrackerListener _tokenTrackingListener;

    public Provider(Context context, Config config, ISessionService sessionService, IAccountService accountService, IPersistenceService persistenceService,
                    IBusinessApiService businessApiService, GigyaLoginCallback gigyaLoginCallback) {
        _context = context;
        _config = config;
        _sessionService = sessionService;
        _accountService = accountService;
        _psService = persistenceService;
        _gigyaLoginCallback = gigyaLoginCallback;
        _businessApiService = businessApiService;

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
                    _businessApiService.refreshNativeProviderSession(params, permissionsCallback);
                }
            };
        }
    }

    @Override
    public void logout() {
        _connecting = false;
    }

    @Override
    public void onCanceled() {
        _connecting = false;
        _gigyaLoginCallback.onOperationCanceled();
    }

    @Override
    public void onLoginSuccess(final Map<String, Object> loginParams, String providerSessions, String loginMode) {
        _connecting = false;
        GigyaLogger.debug(LOG_TAG, "onProviderLoginSuccess: provider = "
                + getName() + ", providerSessions = " + providerSessions);
        // Call intermediate load to give the client the option to trigger his own progress indicator.
        _gigyaLoginCallback.onIntermediateLoad();
        // Setup request.
        loginParams.put("providerSessions", providerSessions);
        loginParams.put("loginMode", loginMode);
        if (loginMode.equals("link") && _regToken != null) {
            loginParams.put("regToken", _regToken);
        }

        Runnable completionHandler = new Runnable() {
            @Override
            public void run() {
                _psService.addSocialProvider(getName());
                if (supportsTokenTracking()) {
                    trackTokenChange();
                }
            }
        };

        // Notify.
        _businessApiService.nativeSocialLogin(loginParams, _gigyaLoginCallback, completionHandler);
    }

    @Override
    public void onLoginFailed(String error) {
        _connecting = false;
        GigyaLogger.debug(LOG_TAG, "onProviderLoginFailed: provider = "
                + getName() + ", error =" + error);
        _gigyaLoginCallback.onError(GigyaError.errorFrom(error));
    }

    @Override
    public void onProviderSession(SessionInfo sessionInfo) {
        _connecting = false;
        // Call intermediate load to give the client the option to trigger his own progress indicator
        _gigyaLoginCallback.onIntermediateLoad();
        // Persist used social provider.
        _psService.addSocialProvider(getName());
        // Set new session.
        _sessionService.setSession(sessionInfo);
        // Force fetch account.
        _accountService.invalidateAccount();

        _businessApiService.getAccount(_gigyaLoginCallback);
    }

    @Override
    public void setRegToken(String regToken) {
        _regToken = regToken;
    }
}
