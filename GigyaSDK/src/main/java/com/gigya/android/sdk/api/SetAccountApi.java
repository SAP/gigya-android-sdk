package com.gigya.android.sdk.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.utils.ObjectUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.gigya.android.sdk.network.GigyaResponse.OK;

public class SetAccountApi<T extends GigyaAccount> extends BaseApi<T>  {

    private static final String API = "accounts.setAccountInfo";

    private Gson gson = new Gson();

    @NonNull
    final private T account, privateAccount;

    public SetAccountApi(@Nullable Class<T> clazz,
                         @NonNull T account, @NonNull T privateAccount) {
        super(clazz);
        this.account = account;
        this.privateAccount = privateAccount;
    }

    /*
    Get account object objectDifference.
     */
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private Map<String, Object> calculateDiff() {
        /* Map updated account object to JSON -> Map. */
        final String updatedJson = gson.toJson(account);
        Map<String, Object> updatedMap = gson.fromJson(updatedJson, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        /* Map original account object to JSON -> Map. */
        final String originalJson = gson.toJson(privateAccount);
        Map<String, Object> originalMap = gson.fromJson(originalJson, new TypeToken<HashMap<String, Object>>() {
        }.getType());

        /* Calculate objectDifference. */
        Map<String, Object> diff = ObjectUtils.objectDifference(originalMap, updatedMap);
        /* Must have UID or regToken. */
        if (updatedMap.containsKey("UID")) {
            diff.put("UID", updatedMap.get("UID"));
        } else if (updatedMap.containsKey("regToken")) {
            diff.put("regToken", updatedMap.get("regToken"));
        }
        serializeObjectFields(diff);
        return diff;
    }


    @SuppressWarnings({"unchecked"})
    public void call(final GigyaCallback callback) {
        GigyaRequest request = new GigyaRequestBuilder(configuration)
                .api(API)
                .sessionManager(sessionManager)
                .params(calculateDiff())
                .build();
        networkAdapter.send(request, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                if (callback == null) {
                    return;
                }
                try {
                    final GigyaResponse response = new GigyaResponse(new JSONObject(jsonResponse));
                    final int statusCode = response.getStatusCode();
                    if (statusCode == OK) {
                        /* Chain a getAccount call */
                        new GetAccountApi<>(clazz).call(callback);
                        return;
                    }
                    final int errorCode = response.getErrorCode();
                    final String localizedMessage = response.getErrorDetails();
                    final String callId = response.getCallId();
                    callback.onError(new GigyaError(response.asJson(), errorCode, localizedMessage, callId));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    callback.onError(GigyaError.generalError());
                }
            }

            @Override
            public void onError(GigyaError gigyaError) {
                if (callback != null) {
                    callback.onError(gigyaError);
                }
            }
        });
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
