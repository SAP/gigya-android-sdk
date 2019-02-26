package com.gigya.android.sdk.api.tfa.phone;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFACompleteVerificationResponse;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFACompleteVerificationApi extends BaseApi<TFACompleteVerificationResponse> {

    private static final String API = "accounts.tfa.phone.completeVerification";

    public TFACompleteVerificationApi(NetworkAdapter networkAdapter,
                                      SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String gigyaAssertion, String phvToken, String code, final GigyaCallback<TFACompleteVerificationResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("phvToken", phvToken);
        params.put("code", code);
        GigyaRequest request = new GigyaRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<TFACompleteVerificationResponse> callback) {
        final TFACompleteVerificationResponse parsed = response.parseTo(TFACompleteVerificationResponse.class);
        callback.onSuccess(parsed);
    }
}
