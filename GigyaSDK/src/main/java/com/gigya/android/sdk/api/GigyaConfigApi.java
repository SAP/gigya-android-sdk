package com.gigya.android.sdk.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaDefinitions;
import com.gigya.android.sdk.model.account.GigyaAccount;
import com.gigya.android.sdk.network.GigyaApiRequest;
import com.gigya.android.sdk.network.GigyaApiRequestBuilder;
import com.gigya.android.sdk.network.GigyaApiResponse;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;
import com.gigya.android.sdk.services.AccountService;
import com.gigya.android.sdk.services.Config;
import com.gigya.android.sdk.services.SessionService;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class GigyaConfigApi<A extends GigyaAccount> extends GigyaApi<GigyaApiResponse, A> {

    public GigyaConfigApi(NetworkAdapter networkAdapter, SessionService sessionService,
                          AccountService<A> accountService) {
        super(networkAdapter, sessionService, accountService, GigyaApiResponse.class);
    }

    public void execute(final GigyaCallback<GigyaApiResponse> callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("include", "permissions,ids,appIds");
        params.put("ApiKey", _sessionService.getConfig().getApiKey());
        // Build request.
        GigyaApiRequest gigyaApiRequest = new GigyaApiRequestBuilder(_sessionService)
                .api(GigyaDefinitions.API.API_GET_SDK_CONFIG).httpMethod(NetworkAdapter.Method.GET).params(params).build();
        send(gigyaApiRequest, callback, true);
    }

    @Override
    public void onRequestSuccess(@NonNull String api, GigyaApiResponse apiResponse, GigyaCallback<GigyaApiResponse> callback) {
        final Config currentSDKConfig = _sessionService.getConfig();
        if (!TextUtils.isEmpty(currentSDKConfig.getApiKey())) {  // Has apiKey.
            final String gmid = apiResponse.getField("ids.gmid", String.class);
            final String ucid = apiResponse.getField("ids.ucid", String.class);
            final JsonObject appIdsJson = apiResponse.getField("appIds", JsonObject.class);
            Type mapType = new TypeToken<Map<String, String>>() {}.getType();
            if (appIdsJson != null) {
                Map<String, String> appIds = apiResponse.getGson().fromJson(appIdsJson.toString(), mapType);
                currentSDKConfig.setAppIds(appIds);
            }
            currentSDKConfig.setGmid(gmid);
            currentSDKConfig.setUcid(ucid);
            // Trigger session save to keep track of gmid/ucid values.
            _sessionService.save();
            _adapter.release();
            if (callback != null) {
                callback.onSuccess(apiResponse);
            }
        }
    }

    @Override
    public void onRequestError(String api, GigyaApiResponse apiResponse, @Nullable GigyaCallback<GigyaApiResponse> callback) {
        super.onRequestError(api, apiResponse, callback);
        _adapter.release();
    }
}
