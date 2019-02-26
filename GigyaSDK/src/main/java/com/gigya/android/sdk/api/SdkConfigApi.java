package com.gigya.android.sdk.api;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.util.HashMap;
import java.util.Map;

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
        GigyaRequest gigyaRequest = new GigyaRequestBuilder(sessionManager).api(API).httpMethod(NetworkAdapter.Method.GET).params(params).build();
        sendRequest(gigyaRequest, API, callback);
    }

    @Override
    protected void onRequestSuccess(String api, GigyaResponse response, GigyaCallback<SdkConfig> callback) {
        final SdkConfig parsed = response.getGson().fromJson(response.asJson(), SdkConfig.class);
        callback.onSuccess(parsed);
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
