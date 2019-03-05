package com.gigya.android.sdk.api.tfa;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFAInitModel;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFAInitApi extends BaseApi<TFAInitModel> {

    private static final String API = "accounts.tfa.initTFA";

    public TFAInitApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String regToken, String provider, String mode, GigyaCallback<TFAInitModel> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        params.put("provider", provider);
        params.put("mode", mode);
        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager)
                .params(params)
                .api(API)
                .build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<TFAInitModel> callback) {
        final TFAInitModel parsed = response.parseTo(TFAInitModel.class);
        callback.onSuccess(parsed);
    }

}
