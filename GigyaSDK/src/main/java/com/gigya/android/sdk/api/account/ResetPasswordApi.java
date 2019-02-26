package com.gigya.android.sdk.api.account;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.Map;

public class ResetPasswordApi extends BaseApi<GigyaResponse> {

    private static final String API = "accounts.resetPassword";

    public ResetPasswordApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(final Map<String, Object> params, final GigyaCallback<GigyaResponse> callback) {
        GigyaRequest gigyaRequest = new GigyaRequestBuilder(sessionManager).api(API).params(params).httpMethod(NetworkAdapter.Method.GET).build();
        sendRequest(gigyaRequest, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<GigyaResponse> callback) {
        callback.onSuccess(response);
    }
}
