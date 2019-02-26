package com.gigya.android.sdk.api.account;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class RefreshProviderSessionApi extends BaseApi<GigyaResponse> {

    private static final String API = "socialize.refreshProviderSession";

    public RefreshProviderSessionApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String providerSession, final GigyaCallback<GigyaResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("providerSession", providerSession);
        GigyaRequest request = new GigyaRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<GigyaResponse> callback) {
        callback.onSuccess(response);
    }
}
