package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class NotifyLoginApi<T> extends BaseApi<T> {

    private static final String API = "socialize.notifyLogin";

    NotifyLoginApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager, @NonNull Class<T> clazz) {
        super(configuration, networkAdapter, sessionManager, clazz);
    }

    public void call(String providerSessions){
        final Map<String, Object> params = new HashMap<>();
        params.put("providerSessions", providerSessions);
        GigyaRequest request = new GigyaRequestBuilder(configuration)
                .sessionManager(sessionManager)
                .params(params)
                .api(API)
                .build();
        networkAdapter.send(request, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {

            }

            @Override
            public void onError(GigyaError gigyaError) {

            }
        });
    }

}
