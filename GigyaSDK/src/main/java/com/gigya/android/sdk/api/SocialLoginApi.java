package com.gigya.android.sdk.api;

import android.support.annotation.NonNull;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;

import java.util.HashMap;
import java.util.Map;

public class SocialLoginApi<T> extends BaseApi<T> {

    private static final String API = "accounts.socialLogin";

    public SocialLoginApi(@NonNull Class<T> clazz) {
        super(clazz);
    }

    public void call(String provider, String providerToken, final GigyaCallback<T> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("x_provider", provider);
        params.put("client_id", configuration.getApiKey());
        params.put("redirect_uri", "gsapi://login_result");
        params.put("response_type", "token");
        GigyaRequest request = new GigyaRequestBuilder(configuration).api(API).params(params).sessionManager(sessionManager).build();
        networkAdapter.send(request, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                if (callback == null) {
                    return;
                }
            }

            @Override
            public void onError(GigyaError gigyaError) {
                if (callback != null) {
                    callback.onError(gigyaError);
                }
            }
        });
    }
}
