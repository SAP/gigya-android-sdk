package com.gigya.android.sdk.api.account;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetConflictingAccountApi extends BaseApi<GigyaResponse> {

    private static final String API = "accounts.getConflictingAccount";

    public GetConflictingAccountApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(String regToken, final GigyaCallback<GigyaResponse> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        GigyaRequest request = new GigyaRequestBuilder(sessionManager).api(API).params(params).build();
        sendRequest(request, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<GigyaResponse> callback) {
        callback.onSuccess(response);
    }

    public static class ConflictingAccount {
        private List<String> loginProviders = new ArrayList<>();
        private String loginID;

        public List<String> getLoginProviders() {
            return loginProviders;
        }

        public String getLoginID() {
            return loginID;
        }
    }
}
