package com.gigya.android.sdk.api.tfa.totp;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFACompleteVerificationModel;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFATOTPVerifyApi extends BaseApi<TFACompleteVerificationModel> {

    private static final String API = "accounts.tfa.totp.verify";

    public TFATOTPVerifyApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String gigyaAssertion, String code, @Nullable String sctToken, GigyaCallback<TFACompleteVerificationModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        if (sctToken != null) {
            params.put("sctToken", sctToken);
        }
        params.put("code", code);
        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).params(params)
                .api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<TFACompleteVerificationModel> callback) {
        final TFACompleteVerificationModel parsed = response.parseTo(TFACompleteVerificationModel.class);
        callback.onSuccess(parsed);
    }
}
