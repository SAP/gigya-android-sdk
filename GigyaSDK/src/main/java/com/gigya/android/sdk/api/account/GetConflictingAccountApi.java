package com.gigya.android.sdk.api.account;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GetConflictingAccountApi extends BaseApi<GigyaApiResponse> {

    private static final String API = "accounts.getConflictingAccount";

    public GetConflictingAccountApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String regToken, final GigyaCallback<GigyaApiResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).api(API).params(params).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<GigyaApiResponse> callback) {
        callback.onSuccess(response);
    }

    public static class ConflictingAccount {
        private ArrayList<String> loginProviders = new ArrayList<>();
        private String loginID;

        public ArrayList<String> getLoginProviders() {
            return loginProviders;
        }

        public String getLoginID() {
            return loginID;
        }
    }
}
