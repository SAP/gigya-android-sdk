package com.gigya.android.sdk.providers.provider;

import com.gigya.android.sdk.api.GigyaApiResponse;
import com.gigya.android.sdk.session.SessionInfo;

import java.util.Map;

public interface ProviderCallback {

    /*
    Used by general web providers -> session info is available.
     */
    void onProviderSession(String providerName, SessionInfo sessionInfo,  Runnable completionHandler);

    /*
    Used by native providers -> requires call to notifySocialLogin endpoint.
     */
    void onProviderSessions(final Map<String, Object> loginParams, Runnable completionHandler);

    /*
    Operation was canceled by the user.
     */
    void onCanceled();

    /*s
    Error on provider login - can be a native provider error or a Gigya specific error if using web provider.
     */
    void onError(GigyaApiResponse errorResponse);
}
