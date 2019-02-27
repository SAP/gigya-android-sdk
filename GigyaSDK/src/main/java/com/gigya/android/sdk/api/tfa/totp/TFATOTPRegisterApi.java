package com.gigya.android.sdk.api.tfa.totp;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFATotpRegisterResponse;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFATOTPRegisterApi extends BaseApi<TFATotpRegisterResponse> {

    private static final String API = "accounts.tfa.totp.register";

    public TFATOTPRegisterApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String gigyaAssertion, GigyaCallback<TFATotpRegisterResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        GigyaRequest request = new GigyaRequestBuilder(sessionManager).params(params)
                .api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<TFATotpRegisterResponse> callback) {
        final TFATotpRegisterResponse parsed = response.parseTo(TFATotpRegisterResponse.class);
        callback.onSuccess(parsed);
    }
}
