package com.gigya.android.sdk.api.account;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.Map;

public class ResetPasswordApi extends BaseApi<GigyaApiResponse> {

    private static final String API = "accounts.resetPassword";

    public ResetPasswordApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(final Map<String, Object> params, final GigyaCallback<GigyaApiResponse> callback) {
        GigyaApiRequest gigyaApiRequest = new GigyaApiRequestBuilder(sessionManager).api(API).params(params).httpMethod(NetworkAdapter.Method.GET).build();
        sendRequest(gigyaApiRequest, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<GigyaApiResponse> callback) {
        callback.onSuccess(response);
    }
}
