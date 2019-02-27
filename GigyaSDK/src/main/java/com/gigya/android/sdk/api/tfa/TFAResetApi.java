package com.gigya.android.sdk.api.tfa;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFAResetApi extends BaseApi<GigyaResponse> {

    private static final String API = "accounts.tfa.resetTFA";

    public TFAResetApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String UID, GigyaCallback<GigyaResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("UID", UID);
        GigyaRequest request = new GigyaRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<GigyaResponse> callback) {
        callback.onSuccess(response);
    }
}
