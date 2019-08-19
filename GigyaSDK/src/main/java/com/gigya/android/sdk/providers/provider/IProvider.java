package com.gigya.android.sdk.providers.provider;

import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.session.SessionInfo;

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

    void onLoginSuccess(final Map<String, Object> loginParams, String providerSessions, String loginMode);

    void onLoginSuccess(String providerName, SessionInfo sessionInfo);

    void onLoginFailed(String error);

    void onLoginFailed(GigyaApiResponse response);
}
