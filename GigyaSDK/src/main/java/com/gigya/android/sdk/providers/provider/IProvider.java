package com.gigya.android.sdk.providers.provider;

import com.gigya.android.sdk.model.account.SessionInfo;

import java.util.Map;

public interface IProvider {

    String getName();

    void login(Map<String, Object> loginParams, String loginMode);

    void logout();

    String getProviderSessions(String tokenOrCode, long expiration, String uid);

    boolean supportsTokenTracking();

    void trackTokenChange();

    void setRegToken(String regToken);

    // Action interfacing.

    void onCanceled();

    void onLoginSuccess(String providerSessions, String loginMode);

    void onLoginFailed(String error);

    void onProviderSession(SessionInfo sessionInfo);
}
