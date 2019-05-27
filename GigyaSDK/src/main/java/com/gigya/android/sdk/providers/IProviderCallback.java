package com.gigya.android.sdk.providers;

import com.gigya.android.sdk.session.SessionInfo;
import com.gigya.android.sdk.providers.provider.Provider;

public interface IProviderCallback {

    void onCanceled();

    void onLoginSuccess(Provider provider, String providerSessions, String loginMethod);

    void onLoginFailed(String provider, String error);

    void onProviderSession(Provider provider, SessionInfo sessionInfo);
}
