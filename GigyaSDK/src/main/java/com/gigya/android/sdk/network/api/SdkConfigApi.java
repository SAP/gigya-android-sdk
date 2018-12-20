package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;

import java.util.HashMap;
import java.util.Map;

public class SdkConfigApi extends BaseApi implements IApi {

    private static final String API = "socialize.getSDKConfig";

    public SdkConfigApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager) {
        super(configuration, sessionManager);
    }

    @SuppressWarnings("unchecked")
    @Override
    public GigyaRequest getRequest(Map<String, Object> params, GigyaCallback callback, GigyaInterceptionCallback interceptor) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("include", "permissions" + (configuration.hasGMID() ? "" : ",ids"));
        params.put("ApiKey", configuration.getApiKey());
        return new GigyaRequestBuilder<SdkConfig>(configuration)
                .api(API)
                .sessionManager(sessionManager)
                .httpMethod(Request.Method.GET)
                .params(params)
                .output(SdkConfig.class)
                .callback(callback)
                .priority(Request.Priority.IMMEDIATE)
                .build();
    }

    public static class SdkConfig {

        Configuration.IDs ids = new Configuration.IDs();

        public Configuration.IDs getIds() {
            return ids;
        }
    }

}
