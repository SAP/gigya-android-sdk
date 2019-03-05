package com.gigya.android.sdk.api.tfa.email;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFAGetEmailsModel;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFAGetEmailsApi extends BaseApi<TFAGetEmailsModel> {

    private static final String API = "accounts.tfa.email.getEmails";

    public TFAGetEmailsApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String gigyaAssertion, GigyaCallback<TFAGetEmailsModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<TFAGetEmailsModel> callback) {
        final TFAGetEmailsModel parsed = response.parseTo(TFAGetEmailsModel.class);
        callback.onSuccess(parsed);
    }
}
