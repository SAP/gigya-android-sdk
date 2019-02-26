package com.gigya.android.sdk.api.tfa;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFAProvidersResponse;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFAGetProvidersApi extends BaseApi<TFAProvidersResponse> {

    private static final String API = "accounts.tfa.getProviders";

    public TFAGetProvidersApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String regToken, final GigyaCallback<TFAProvidersResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        GigyaRequest request = new GigyaRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<TFAProvidersResponse> callback) {
        final TFAProvidersResponse parsed = response.parseTo(TFAProvidersResponse.class);
        callback.onSuccess(parsed);
    }
}
