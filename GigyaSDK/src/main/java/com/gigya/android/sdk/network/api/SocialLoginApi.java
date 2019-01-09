package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class SocialLoginApi<T> extends BaseApi<T> {

    private static final String API = "accounts.socialLogin";

    public SocialLoginApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter,
                          @Nullable SessionManager sessionManager, @NonNull Class<T> clazz) {
        super(configuration, networkAdapter, sessionManager, clazz);
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