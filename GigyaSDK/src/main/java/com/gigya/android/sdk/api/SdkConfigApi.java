package com.gigya.android.sdk.api;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.gigya.android.sdk.network.GigyaApiResponse.OK;

public class SdkConfigApi extends BaseApi<SdkConfigApi.SdkConfig> {

    private static final String API = "socialize.getSDKConfig";

    public SdkConfigApi(NetworkAdapter networkAdapter, SessionManager sessionManager) {
        super(networkAdapter, sessionManager);
    }

    public void call(final GigyaCallback<SdkConfig> callback) {
        final Configuration configuration = sessionManager.getConfiguration();
        final Map<String, Object> params = new HashMap<>();
        params.put("include", "permissions,ids,appIds");
        params.put("ApiKey", configuration.getApiKey());
        // Build request.
        GigyaApiRequest gigyaApiRequest = new GigyaApiRequestBuilder(sessionManager).api(API).httpMethod(NetworkAdapter.Method.GET).params(params).build();
        networkAdapter.sendBlocking(gigyaApiRequest, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                if (callback == null) {
                    return;
                }
                try {
                    final GigyaApiResponse response = new GigyaApiResponse(new JSONObject(jsonResponse));
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

        private Configuration.IDs ids = new Configuration.IDs();
        private Map<String, String> appIds = new HashMap<>();

        public Configuration.IDs getIds() {
            return ids;
        }

        public Map<String, String> getAppIds() {
            return appIds;
        }
    }


}
