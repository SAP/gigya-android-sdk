package com.gigya.android.sdk.api.tfa.phone;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFAVerificationCodeModel;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFASendVerificationCodeApi extends BaseApi<TFAVerificationCodeModel> {

    private static final String API = "accounts.tfa.phone.sendVerificationCode";

    public TFASendVerificationCodeApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String gigyaAssertion, String phone, String method, String lang, boolean isId, final GigyaCallback<TFAVerificationCodeModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put(isId ? "phoneID": "phone", phone);
        params.put("method", method);
        params.put("lang", lang);
        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<TFAVerificationCodeModel> callback) {
        final TFAVerificationCodeModel parsed = response.parseTo(TFAVerificationCodeModel.class);
        callback.onSuccess(parsed);
    }
}
