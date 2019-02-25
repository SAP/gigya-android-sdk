package com.gigya.android.sdk.api.tfa;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;

import java.util.HashMap;
import java.util.Map;

public class TFAFinalizeApi extends BaseApi<GigyaResponse> {

    private static final String API = "accounts.tfa.finalizeTFA";

    public void call(String gigyaAssertion, String providerAssertion, String regToken, GigyaCallback<GigyaResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("providerAssertion", providerAssertion);
        params.put("regToken", regToken);
        GigyaRequest request = new GigyaRequestBuilder(configuration)
                .params(params)
                .api(API)
                .sessionManager(sessionManager)
                .build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<GigyaResponse> callback) {
        callback.onSuccess(response);
    }
}
