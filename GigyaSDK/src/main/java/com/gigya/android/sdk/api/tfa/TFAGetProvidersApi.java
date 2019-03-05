package com.gigya.android.sdk.api.tfa;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFAProvidersModel;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFAGetProvidersApi extends BaseApi<TFAProvidersModel> {

    private static final String API = "accounts.tfa.getProviders";

    public TFAGetProvidersApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String regToken, final GigyaCallback<TFAProvidersModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<TFAProvidersModel> callback) {
        final TFAProvidersModel parsed = response.parseTo(TFAProvidersModel.class);
        callback.onSuccess(parsed);
    }
}
