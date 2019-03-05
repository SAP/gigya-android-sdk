package com.gigya.android.sdk.api.account;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.api.BaseApi;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.utils.ObjectUtils;

public class GetAccountApi<T extends GigyaAccount> extends BaseApi<T> {

    private final Class<T> clazz;
    protected final AccountManager accountManager;

    public GetAccountApi(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager) {
        super(networkAdapter, sessionManager);
        this.accountManager = accountManager;
        this.clazz = accountManager.getAccountClazz();
    }

    private static final String API = "accounts.getAccountInfo";

    public void call(final GigyaCallback callback) {
        GigyaApiRequest gigyaApiRequest = new GigyaApiRequestBuilder(sessionManager)
                .api(API)
                .build();
        sendRequest(gigyaApiRequest, API, callback);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onRequestSuccess(String api, GigyaApiResponse response, GigyaCallback<T> callback) {
        final String json = response.asJson();
        final T parsed = response.getGson().fromJson(json, clazz);
        accountManager.setAccount(ObjectUtils.deepCopy(response.getGson(), parsed, clazz));
        callback.onSuccess(parsed);
    }
}
