package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.model.BaseGigyaAccount;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.SessionManager;

import java.util.Map;

public class GetAccountApi<T> extends BaseApi<T> implements IApi {

    private static final String API = "accounts.getAccountInfo";

    public GetAccountApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable Class<T> clazz) {
        super(configuration, sessionManager, clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public GigyaRequest getRequest(Map<String, Object> params, GigyaCallback callback, GigyaInterceptionCallback interceptor) {
        return new GigyaRequestBuilder<T>(configuration)
                .sessionManager(sessionManager)
                .api(API)
                .output(this.clazz)
                .callback(callback)
                .interceptor(interceptor)
                .build();
    }
}
