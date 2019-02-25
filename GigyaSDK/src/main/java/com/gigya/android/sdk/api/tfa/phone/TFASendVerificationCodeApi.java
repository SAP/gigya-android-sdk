package com.gigya.android.sdk.api.tfa.phone;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFAVerificationCodeResponse;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;

import java.util.HashMap;
import java.util.Map;

public class TFASendVerificationCodeApi extends BaseApi<TFAVerificationCodeResponse> {

    private static final String API = "accounts.tfa.phone.sendVerificationCode";

    public void call(String gigyaAssertion, int phone, String method, String lang, final GigyaCallback<TFAVerificationCodeResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("phone", phone);
        params.put("method", method);
        params.put("lang", lang);
        GigyaRequest request = new GigyaRequestBuilder(configuration)
                .params(params)
                .api(API)
                .sessionManager(sessionManager)
                .build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<TFAVerificationCodeResponse> callback) {
        final TFAVerificationCodeResponse parsed = response.parseTo(TFAVerificationCodeResponse.class);
        callback.onSuccess(parsed);
    }
}
