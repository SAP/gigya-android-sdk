package com.gigya.android.sdk.api.tfa.phone;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFAVerificationCodeResponse;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFASendVerificationCodeApi extends BaseApi<TFAVerificationCodeResponse> {

    private static final String API = "accounts.tfa.phone.sendVerificationCode";

    public TFASendVerificationCodeApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String gigyaAssertion, String phone, String method, String lang, boolean isId, final GigyaCallback<TFAVerificationCodeResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put(isId ? "phoneID": "phone", phone);
        params.put("method", method);
        params.put("lang", lang);
        GigyaRequest request = new GigyaRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<TFAVerificationCodeResponse> callback) {
        final TFAVerificationCodeResponse parsed = response.parseTo(TFAVerificationCodeResponse.class);
        callback.onSuccess(parsed);
    }
}
