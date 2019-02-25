package com.gigya.android.sdk.api.tfa;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFAInitResponse;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;

import java.util.HashMap;
import java.util.Map;

public class TFAInitApi extends BaseApi<TFAInitResponse> {

    private static final String API = "accounts.tfa.initTFA";

    public void call(String regToken, String provider, String mode, GigyaCallback<TFAInitResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        params.put("provider", provider);
        params.put("mode", mode);
        GigyaRequest request = new GigyaRequestBuilder(configuration)
                .params(params)
                .api(API)
                .sessionManager(sessionManager)
                .build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<TFAInitResponse> callback) {
        final TFAInitResponse parsed = response.parseTo(TFAInitResponse.class);
        callback.onSuccess(parsed);
    }

}
