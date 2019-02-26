package com.gigya.android.sdk.api.account;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.utils.ObjectUtils;

public class GetAccountApi<T extends GigyaAccount> extends BaseApi<T> {

    private final Class<T> clazz;
    protected final AccountManager accountManager;

    public GetAccountApi(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager,
                         Class<T> clazz) {
        super(networkAdapter, sessionManager);
        this.accountManager = accountManager;
        this.clazz = clazz;
    }

    private static final String API = "accounts.getAccountInfo";

    public void call(final GigyaCallback callback) {
        GigyaRequest gigyaRequest = new GigyaRequestBuilder(sessionManager)
                .api(API)
                .build();
        sendRequest(gigyaRequest, API, callback);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<T> callback) {
        final String json = response.asJson();
        final T parsed = response.getGson().fromJson(json, clazz);
        accountManager.setAccount(ObjectUtils.deepCopy(response.getGson(), parsed, clazz));
        callback.onSuccess(parsed);
    }
}
