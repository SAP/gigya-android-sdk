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
import java.util.Set;

public class SetAccountApi<T> extends BaseApi<T> implements IApi {

    private static final String API = "accounts.setAccountInfo";

    private Gson gson = new Gson();

    @NonNull
    final private T account, privateAccount;

    public SetAccountApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @NonNull T account, @NonNull T privateAccount) {
        super(configuration, sessionManager);
        this.account = account;
        this.privateAccount = privateAccount;
    }

    @SuppressWarnings("unchecked")
    @Override
    public GigyaRequest getRequest(Map<String, Object> params, GigyaCallback callback, GigyaInterceptionCallback interceptor) {

        // Map updated account object to JSON -> Map.
        final String updatedJson = gson.toJson(account);
        Map<String, Object> updatedMap = gson.fromJson(updatedJson, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        // Map original account object to JSON -> Map.
        final String originalJson = gson.toJson(privateAccount);
        Map<String, Object> originalMap = gson.fromJson(originalJson, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        // Calculate difference.
        Map<String, Object> diff = difference(updatedMap, originalMap);
        // Must have UID or regToken.
        if (updatedMap.containsKey("UID")) {
            diff.put("UID", updatedMap.get("UID"));
        }
        else if (updatedMap.containsKey("regToken")) {
            diff.put("regToken", updatedMap.get("regToken"));
        }
        serializeObjectFields(diff);

        return new GigyaRequestBuilder(configuration)
                .sessionManager(sessionManager)
                .api(API)
                .params(diff)
                .callback(callback)
                .interceptor(interceptor)
                .build();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> difference(Map<String, Object> updated, Map<String, Object> original) {
        Map<String, Object> result = new HashMap<>();
        Set<Map.Entry<String, Object>> filter = original.entrySet();
        for (Map.Entry<String, Object> entry : updated.entrySet()) {
            if (!filter.contains(entry)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<String, Object> item : result.entrySet()) {
            final String key = item.getKey();
            final Object value = item.getValue();
            if (value instanceof Map) {
                if (updated.get(key) != null && original.get(key) != null) {
                    Map<String, Object> childResult = difference((Map<String, Object>) updated.get(key), (Map<String, Object>) original.get(key));
                    result.put(key, childResult);
                }
            }
        }

        return result;
    }

    /*
       Object represented field values must be set as JSON Objects. So we are reverting each one to
        its JSON String representation.
         */
    private void serializeObjectFields(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue() instanceof Map) {
                map.put(entry.getKey(), gson.toJson(entry.getValue()));
            }
        }
    }
}
