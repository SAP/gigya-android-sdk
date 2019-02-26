package com.gigya.android.sdk.api.account;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.utils.ObjectUtils;

import java.util.HashMap;
import java.util.Map;

public class FinalizeRegistrationApi<T extends GigyaAccount> extends BaseApi<T> {

    private static final String API = "accounts.finalizeRegistration";

    private final Class<T> clazz;
    private final AccountManager accountManager;

    public FinalizeRegistrationApi(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager) {
        super(networkAdapter, sessionManager);
        this.accountManager = accountManager;
        this.clazz = accountManager.getAccountClazz();
    }

    public void call(String regToken, GigyaCallback<T> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("regToken", regToken);
        params.put("include", "profile,data,emails,subscriptions,preferences");
        params.put("includeUserInfo", "true");
        GigyaRequest request = new GigyaRequestBuilder(sessionManager)
                .api(API)
                .params(params)
                .build();
        sendRequest(request, API, callback);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<T> callback) {
        if (response.contains("sessionInfo") && sessionManager != null) {
            SessionInfo session = response.getField("sessionInfo", SessionInfo.class);
            sessionManager.setSession(session);
        }
        final String json = response.asJson();
        final T parsed = response.getGson().fromJson(json, clazz);
        accountManager.setAccount(ObjectUtils.deepCopy(response.getGson(), parsed, clazz));
        callback.onSuccess(parsed);
    }
}
