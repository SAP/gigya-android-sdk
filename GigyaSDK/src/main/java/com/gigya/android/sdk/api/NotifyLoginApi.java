package com.gigya.android.sdk.api;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.SessionInfo;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequest;
import com.gigya.android.sdk.network.GigyaRequestBuilder;
import com.gigya.android.sdk.network.GigyaResponse;
import com.gigya.android.sdk.network.adapter.INetworkCallbacks;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import static com.gigya.android.sdk.network.GigyaResponse.OK;

public class NotifyLoginApi<T extends GigyaAccount> extends BaseApi<T> {

    public NotifyLoginApi(Class<T> clazz) {
        super(clazz);
    }

    private static final String API = "socialize.notifyLogin";

    public void call(SessionInfo sessionInfo, final GigyaCallback<T> callback, final GigyaInterceptionCallback<T> interceptor) {
        // Update session.
        if (sessionManager != null) {
            sessionManager.setSession(sessionInfo);
        }
        // Request account info.
        getAccount(callback, interceptor);
    }

    public void call(String providerSessions, final GigyaCallback<T> callback, final GigyaInterceptionCallback<T> interceptor) {
        final Map<String, Object> params = new HashMap<>();
        params.put("providerSessions", providerSessions);
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
                        /* Update session info */
                        if (sessionManager != null && response.contains("sessionSecret") && response.contains("sessionToken")) {
                            updateSessionAndRequestAccountInfo(response, callback, interceptor);
                        }
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

    private void updateSessionAndRequestAccountInfo(GigyaResponse notifyResponse, final GigyaCallback<T> callback, final GigyaInterceptionCallback<T> interceptor) {
        /* Parse session info and request account info. */
        SessionInfo sessionInfo;
        final String sessionSecret = notifyResponse.getField("sessionSecret", String.class);
        final String sessionToken = notifyResponse.getField("sessionToken", String.class);
        final Long expirationTime = notifyResponse.getField("expirationTime", Long.class);
        if (expirationTime != null) {
            sessionInfo = new SessionInfo(sessionSecret, sessionToken, expirationTime);
        } else {
            sessionInfo = new SessionInfo(sessionSecret, sessionToken);
        }

        // Update session.
        if (sessionManager != null) {
            sessionManager.setSession(sessionInfo);
        }

        // Request account info.
        getAccount(callback, interceptor);
    }

    private void getAccount(final GigyaCallback<T> callback, final GigyaInterceptionCallback<T> interceptor) {
        // Request account info.
        new GetAccountApi<>(clazz)
                .call(new GigyaCallback<T>() {
                    @Override
                    public void onSuccess(T obj) {
                        if (interceptor != null) {
                            interceptor.intercept(obj);
                        }
                        callback.onSuccess(obj);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        callback.onError(error);
                    }
                });
    }
}
