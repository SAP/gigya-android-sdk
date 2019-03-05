package com.gigya.android.sdk.api.tfa.phone;

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

public class TFACompleteVerificationApi extends BaseApi<TFACompleteVerificationModel> {

    private static final String API = "accounts.tfa.phone.completeVerification";

    public TFACompleteVerificationApi(NetworkAdapter networkAdapter,
                                      SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String gigyaAssertion, String phvToken, String code, final GigyaCallback<TFACompleteVerificationModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        params.put("phvToken", phvToken);
        params.put("code", code);
        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<TFACompleteVerificationModel> callback) {
        final TFACompleteVerificationModel parsed = response.parseTo(TFACompleteVerificationModel.class);
        callback.onSuccess(parsed);
    }
}
