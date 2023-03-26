package com.gigya.android.sdk.providers.external;

import java.util.Map;

public interface IProviderWrapperCallback {

    void onLogin(Map<String, Object> loginParams);

    void onCanceled();

    void onFailed(String withError);
}
