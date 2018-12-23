package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.model.BaseGigyaAccount;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.SessionManager;

import java.util.Map;

public class LoginApi<T> extends BaseApi<T> implements IApi {

    private static final String API = "accounts.login";

    public LoginApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable Class<T> clazz) {
        super(configuration, sessionManager, clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public GigyaRequest getRequest(Map<String, Object> params, GigyaCallback callback, GigyaInterceptionCallback interceptor) {
        params.put("ApiKey", configuration.getApiKey());
        return new GigyaRequestBuilder<BaseGigyaAccount>(configuration)
                .sessionManager(sessionManager)
                .api(API)
                .httpMethod(Request.Method.GET)
                .params(params)
                .output(clazz)
                .callback(callback)
                .build();
    }
}
