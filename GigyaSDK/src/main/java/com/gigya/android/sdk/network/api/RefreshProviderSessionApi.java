package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class RefreshProviderSessionApi extends BaseApi {

    RefreshProviderSessionApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager) {
        super(configuration, networkAdapter, sessionManager);
    }

    public void call(String providerSession) {
        Map<String, Object> params = new HashMap<>();
        params.put("providerSession", providerSession);

    }
}
