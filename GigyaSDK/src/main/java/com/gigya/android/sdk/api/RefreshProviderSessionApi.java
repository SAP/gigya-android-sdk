package com.gigya.android.sdk.api;

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

public class RefreshProviderSessionApi extends BaseApi {

    private static final String API = "socialize.refreshProviderSession";

    public RefreshProviderSessionApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager) {
        super(configuration, networkAdapter, sessionManager);
    }

    public void call(String providerSession, final GigyaCallback callback) {
        Map<String, Object> params = new HashMap<>();
        params.put("providerSession", providerSession);
        GigyaRequest request = new GigyaRequestBuilder(configuration)
                .sessionManager(sessionManager)
                .params(params)
                .api(API)
                .build();
        networkAdapter.send(request, new INetworkCallbacks() {
            @SuppressWarnings("unchecked")
            @Override
            public void onResponse(String jsonResponse) {
                if (callback == null) {
                    return;
                }
                try {
                    final GigyaResponse response = new GigyaResponse(new JSONObject(jsonResponse));
                    final int statusCode = response.getStatusCode();
                    if (statusCode == OK) {
                        callback.onSuccess(response);
                        return;
                    }
                    final int errorCode = response.getErrorCode();
                    final String localizedMessage = response.getErrorDetails();
                    final String callId = response.getCallId();
                    callback.onError(new GigyaError(errorCode, localizedMessage, callId));
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


}
