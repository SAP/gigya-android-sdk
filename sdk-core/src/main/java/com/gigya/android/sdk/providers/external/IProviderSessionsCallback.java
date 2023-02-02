package com.gigya.android.sdk.providers.external;

import java.util.Map;

public interface IProviderSessionsCallback {

    void onProviderSessions(Map<String, Object> loginParams, Runnable completionHandler);

    void onMissingExternalProvider();
}
