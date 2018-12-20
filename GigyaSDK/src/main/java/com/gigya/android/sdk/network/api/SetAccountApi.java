package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.BaseGigyaAccount;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.Map;

public class SetAccountApi<T> extends BaseApi<T> implements IApi {

    private static final String API = "accounts.setAccountInfo";

    private Gson gson = new Gson();

    @NonNull
    final private T account;

    public SetAccountApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @NonNull T account) {
        super(configuration, sessionManager);
        this.account = account;
    }

    @SuppressWarnings("unchecked")
    @Override
    public GigyaRequest getRequest(Map<String, Object> params, GigyaCallback callback, GigyaInterceptionCallback interceptor) {
        // Map account object to JSON -> Map.
        final String json = gson.toJson(account);
        params = gson.fromJson(json, new TypeToken<HashMap<String, Object>>() {
        }.getType());


        // Object represented field values must be set as JSON Objects. So we are reverting each one to
        // its JSON String representation.
        serializeObjectFields(params);

        return new GigyaRequestBuilder(configuration)
                .sessionManager(sessionManager)
                .api(API)
                .params(params)
                .callback(callback)
                .interceptor(interceptor)
                .build();
    }

    private Map<String, Object> calculateDeltas(Map<String, Object> params) {
        Map<String, Object> toUpdateParams = new HashMap<>();



        return toUpdateParams;
    }

    private void serializeObjectFields(Map<String, Object> params) {
        if (params.containsKey("profile")) {
            final String profileJson = gson.toJson(params.get("profile"));
            params.put("profile", profileJson);
        }
        if (params.containsKey("data")) {
            final String dataJson = gson.toJson(params.get("data"));
            params.put("data", dataJson);
        }
        if (params.containsKey("preferences")) {
            final String preferencesJson = gson.toJson(params.get("preferences"));
            params.put("preferences", preferencesJson);
        }
        if (params.containsKey("subscriptions")) {
            final String subscriptionsJson = gson.toJson(params.get("subscriptions"));
            params.put("subscriptions", subscriptionsJson);
        }
        if (params.containsKey("rba")) {
            final String rbaJson = gson.toJson(params.get("rba"));
            params.put("rba", rbaJson);
        }
    }
}
