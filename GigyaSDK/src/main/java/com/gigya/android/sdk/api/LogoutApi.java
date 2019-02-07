package com.gigya.android.sdk.api;

import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;

public class LogoutApi extends BaseApi {

    private static final String API = "socialize.logout";

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
