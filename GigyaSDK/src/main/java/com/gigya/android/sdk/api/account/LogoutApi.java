package com.gigya.android.sdk.api.account;

import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public class LogoutApi extends BaseApi<GigyaResponse> {

    private static final String API = "socialize.logout";

    private final SessionManager sessionManager;

    public LogoutApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
        this.sessionManager = sessionManager;
    }

    public void call() {
        GigyaRequest request = new GigyaRequestBuilder(sessionManager).api(API).build();
        sendRequest(request, API, null);
    }
}
