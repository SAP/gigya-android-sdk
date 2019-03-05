package com.gigya.android.sdk.api.tfa.phone;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.tfa.TFAGetRegisteredPhoneNumbersModel;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

public class TFAGetRegisteredPhoneNumbersAPi extends BaseApi<TFAGetRegisteredPhoneNumbersModel> {

    private static final String API = "accounts.tfa.phone.getRegisteredPhoneNumbers";

    public TFAGetRegisteredPhoneNumbersAPi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String gigyaAssertion, final GigyaCallback<TFAGetRegisteredPhoneNumbersModel> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("gigyaAssertion", gigyaAssertion);
        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).params(params).api(API).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<TFAGetRegisteredPhoneNumbersModel> callback) {
        final TFAGetRegisteredPhoneNumbersModel parsed = response.parseTo(TFAGetRegisteredPhoneNumbersModel.class);
        callback.onSuccess(parsed);
    }
}
