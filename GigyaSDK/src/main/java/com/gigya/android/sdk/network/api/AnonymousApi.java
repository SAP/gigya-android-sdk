package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.Map;

import static com.gigya.android.sdk.network.GigyaResponse.OK;


@SuppressWarnings("unchecked")
public class AnonymousApi<H> extends BaseApi<H> {

    public AnonymousApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager) {
        super(configuration, networkAdapter, sessionManager);
    }

    public AnonymousApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager, @Nullable Class<H> clazz) {
        super(configuration, networkAdapter, sessionManager, clazz);
    }

    public void call(String api, Map<String, Object> params, final GigyaCallback<H> callback) {
        GigyaRequest request = new GigyaRequestBuilder(configuration)
                .params(params)
                .api(api)
                .sessionManager(sessionManager)
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
                        Gson gson = new Gson();
                        if (clazz == null) {
                            /* Callback will return GigyaResponse instance */
                            callback.onSuccess((H) response);
                            return;
                        } else {
                            H parsed = gson.fromJson(jsonResponse, clazz);
                            callback.onSuccess(parsed);
                            return;
                        }
                    }
                    final int errorCode = response.getErrorCode();
                    final String localizedMessage = response.getErrorDetails();
                    final String callId = response.getCallId();
                    callback.onError(new GigyaError(errorCode, localizedMessage, callId));
                } catch (Exception ex) {
                    ex.printStackTrace();
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
}
