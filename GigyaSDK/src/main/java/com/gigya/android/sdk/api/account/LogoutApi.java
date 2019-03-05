package com.gigya.android.sdk.api.account;

import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public class LogoutApi extends BaseApi<GigyaApiResponse> {

    private static final String API = "socialize.logout";

    private final SessionManager sessionManager;

    public LogoutApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
        this.sessionManager = sessionManager;
    }

    public void call() {
        GigyaApiRequest request = new GigyaApiRequestBuilder(sessionManager).api(API).build();
        sendRequest(request, API, null);
    }
}
