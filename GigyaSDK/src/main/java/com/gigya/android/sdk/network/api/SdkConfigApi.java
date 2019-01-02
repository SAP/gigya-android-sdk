package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaRequestBuilderOld;
import com.gigya.android.sdk.network.GigyaRequestOld;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.gigya.android.sdk.network.GigyaResponse.OK;

public class SdkConfigApi extends BaseApi implements IApi {

    private static final String API = "socialize.getSDKConfig";

    @Deprecated
    public SdkConfigApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager) {
        super(configuration, sessionManager);

    }

    public SdkConfigApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager) {
        super(configuration, networkAdapter, sessionManager);
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    @Override
    public GigyaRequestOld getRequest(Map<String, Object> params, GigyaCallback callback, GigyaInterceptionCallback interceptor) {
        if (params == null) {
            params = new HashMap<>();
        }
        params.put("include", "permissions" + (configuration.hasGMID() ? "" : ",ids"));
        params.put("ApiKey", configuration.getApiKey());
        return new GigyaRequestBuilderOld<SdkConfig>(configuration)
                .api(API)
                .sessionManager(sessionManager)
                .httpMethod(Request.Method.GET)
                .params(params)
                .output(SdkConfig.class)
                .callback(callback)
                .priority(Request.Priority.IMMEDIATE)
                .build();
    }

    public void call(final GigyaCallback<SdkConfig> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("include", "permissions" + (configuration.hasGMID() ? "" : ",ids"));
        params.put("ApiKey", configuration.getApiKey());
        // Build request.
        GigyaRequest gigyaRequest = new GigyaRequestBuilder(configuration)
                .api(API)
                .httpMethod(NetworkAdapter.Method.GET)
                .params(params)
                .sessionManager(sessionManager)
                .build();
        // Send request.
        networkAdapter.send(gigyaRequest, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                if (callback == null) {
                    return;
                }
                try {
                    final GigyaResponse response = new GigyaResponse(new JSONObject(jsonResponse));
                    final int statusCode = response.getStatusCode();
                    if (statusCode == OK) {
                        final SdkConfig parsed = new Gson().fromJson(jsonResponse, SdkConfig.class);
                        callback.onSuccess(parsed);
                        return;
                    }
                    // Error handling.
                    final int errorCode = response.getErrorCode();
                    final String localizedMessage = response.getErrorDetails();
                    final String callId = response.getCallId();
                    callback.onError(new GigyaError(errorCode, localizedMessage, callId));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    // TODO: 31/12/2018 Need to define general error (what it contains).
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
        // Blocking network queue.
        networkAdapter.block();
    }

    /*
    Helper class.
     */
    public static class SdkConfig {

        Configuration.IDs ids = new Configuration.IDs();

        public Configuration.IDs getIds() {
            return ids;
        }
    }

}
