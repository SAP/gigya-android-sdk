package com.gigya.android.sdk.api.tfa.totp;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFATotpRegisterModel;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFATOTPRegisterApi extends BaseApi<TFATotpRegisterModel> {

    private static final String API = "accounts.tfa.totp.register";

    public TFATOTPRegisterApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String gigyaAssertion, GigyaCallback<TFATotpRegisterModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).params(params)
                .api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<TFATotpRegisterModel> callback) {
        final TFATotpRegisterModel parsed = response.parseTo(TFATotpRegisterModel.class);
        callback.onSuccess(parsed);
    }
}
