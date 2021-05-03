package com.gigya.android.sdk.providers.provider;

import android.content.Context;

import com.gigya.android.sdk.GigyaLogger;
import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.persistence.IPersistenceService;
import com.gigya.android.sdk.session.SessionInfo;

import java.util.Map;

public abstract class Provider implements IProvider {

    private static final String LOG_TAG = "Provider";

    final protected Context _context;
    private final IPersistenceService _psService;

    boolean _connecting = false;

    // Dynamic
    String _loginMode;

    private String _regToken;

    ProviderCallback _providerCallback;

    public Provider(Context context, IPersistenceService persistenceService, ProviderCallback providerCallback) {
        _context = context;
        _psService = persistenceService;
        _providerCallback = providerCallback;
    }

    @Override
    public void logout() {
        _connecting = false;
    }

    @Override
    public void onCanceled() {
        _connecting = false;

        // Trigger cancellation callback.
        _providerCallback.onCanceled();
    }

    @Override
    public void onLoginSuccess(final Map<String, Object> loginParams, String providerSessions, String loginMode) {
        _connecting = false;
        GigyaLogger.debug(LOG_TAG, "onLoginSuccess: provider = "
                + getName() + ", providerSessions = " + providerSessions);

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
            }
        };

        // Notify.
        _providerCallback.onProviderSessions(loginParams, completionHandler);
    }

    @Override
    public void onLoginSuccess(String providerName, SessionInfo sessionInfo) {
        _connecting = false;

        GigyaLogger.debug(LOG_TAG, "onLoginSuccess: provider = "
                + getName() + ", sessionToken = " + sessionInfo.getSessionToken());

        Runnable completionHandler = new Runnable() {
            @Override
            public void run() {
                _psService.addSocialProvider(getName());
            }
        };

        //Notify.
        _providerCallback.onProviderSession(providerName, sessionInfo, completionHandler);
    }

    @Override
    public void onLoginFailed(String error) {
        _connecting = false;
        GigyaLogger.debug(LOG_TAG, "onProviderLoginFailed: provider = "
                + getName() + ", error =" + error);

        // Notify.
        final String json = GigyaError.errorFrom(error).getData();
        _providerCallback.onError(new GigyaApiResponse(json));
    }

    @Override
    public void onLoginFailed(GigyaApiResponse response) {
        _connecting = false;
        GigyaLogger.debug(LOG_TAG, "onProviderLoginFailed: provider = "
                + getName() + ", error =" + response.toString());

        // Notify.
        _providerCallback.onError(response);
    }

    @Override
    public void setRegToken(String regToken) {
        _regToken = regToken;
    }
}
