package com.gigya.android.sdk.api;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;

import org.json.JSONObject;

import java.util.Map;

import static com.gigya.android.sdk.network.GigyaResponse.OK;


@SuppressWarnings("unchecked")
public class AnonymousApi<H> extends BaseApi<H> {

    public AnonymousApi() {
        super();
    }

    public AnonymousApi(Class<H> clazz) {
        super(clazz);
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
                        if (sessionManager != null && response.contains("sessionSecret") && response.contains("sessionToken")) {
                            SessionInfo session = response.getField("sessionInfo", SessionInfo.class);
                            sessionManager.setSession(session);
                        }
                        if (clazz == null) {
                            /* Callback will return GigyaResponse instance */
                            callback.onSuccess((H) response);
                            return;
                        } else {
                            H parsed = response.getGson().fromJson(jsonResponse, clazz);
                            callback.onSuccess(parsed);
                            return;
                        }
                    }
                    final int errorCode = response.getErrorCode();
                    final String localizedMessage = response.getErrorDetails();
                    final String callId = response.getCallId();
                    callback.onError(new GigyaError(response.asJson(), errorCode, localizedMessage, callId));
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
