package com.gigya.android.sdk.api.tfa.phone;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFAGetRegisteredPhoneNumbersResponse;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFAGetRegisteredPhoneNumbersAPi extends BaseApi<TFAGetRegisteredPhoneNumbersResponse> {

    private static final String API = "accounts.tfa.phone.getRegisteredPhoneNumbers";

    public TFAGetRegisteredPhoneNumbersAPi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String gigyaAssertion, final GigyaCallback<TFAGetRegisteredPhoneNumbersResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        GigyaRequest request = new GigyaRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<TFAGetRegisteredPhoneNumbersResponse> callback) {
        final TFAGetRegisteredPhoneNumbersResponse parsed = response.parseTo(TFAGetRegisteredPhoneNumbersResponse.class);
        callback.onSuccess(parsed);
    }
}
