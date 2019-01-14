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

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.gigya.android.sdk.network.GigyaResponse.OK;

public class SdkConfigApi extends BaseApi {

    private static final String API = "socialize.getSDKConfig";
    private final String API_INCLUDES = "permissions,ids,appIds";

    public SdkConfigApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager) {
        super(configuration, networkAdapter, sessionManager);
    }

    public void call(final GigyaCallback<SdkConfig> callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("include", API_INCLUDES);
        params.put("ApiKey", configuration.getApiKey());
        // Build request.
        GigyaRequest gigyaRequest = new GigyaRequestBuilder(configuration)
                .api(API)
                .httpMethod(NetworkAdapter.Method.GET)
                .params(params)
                .sessionManager(sessionManager)
                .build();
        // Send request.
        networkAdapter.sendBlocking(gigyaRequest, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                if (callback == null) {
                    return;
                }
                try {
                    final GigyaResponse response = new GigyaResponse(new JSONObject(jsonResponse));
                    final int statusCode = response.getStatusCode();
                    if (statusCode == OK) {
                        final SdkConfig parsed = response.getGson().fromJson(jsonResponse, SdkConfig.class);
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
    }

    /*
    Helper class.
     */
    public static class SdkConfig {

        Configuration.IDs ids = new Configuration.IDs();

        Map<String, String> appIds = new HashMap<>();

        public Configuration.IDs getIds() {
            return ids;
        }

        public Map<String, String> getAppIds() {
            return appIds;
        }
    }


}
