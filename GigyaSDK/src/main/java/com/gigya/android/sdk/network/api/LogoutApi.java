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

public class LogoutApi extends BaseApi {

    private static final String API = "socialize.logout";

    public LogoutApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager) {
        super(configuration, networkAdapter, sessionManager);
    }

    public void call() {
        GigyaRequest request = new GigyaRequestBuilder(configuration)
                .api(API)
                .sessionManager(sessionManager)
                .build();
        networkAdapter.send(request, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                // Ignored.
            }

            @Override
            public void onError(GigyaError gigyaError) {
                // Ignored.
            }
        });
    }
}
