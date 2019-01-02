package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.Request;
import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.model.GigyaAccount;
import com.gigya.android.sdk.model.SessionInfo;
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

import java.util.Map;

import static com.gigya.android.sdk.network.GigyaResponse.OK;


@SuppressWarnings("unchecked")
public class LoginApi<T> extends BaseApi<T> implements IApi {

    private static final String API = "accounts.login";

    @Deprecated
    public LoginApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable Class<T> clazz) {
        super(configuration, sessionManager, clazz);
    }

    public LoginApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager, @Nullable Class<T> clazz) {
        super(configuration, networkAdapter, sessionManager, clazz);
    }

    @Deprecated
    @SuppressWarnings("unchecked")
    @Override
    public GigyaRequestOld getRequest(Map<String, Object> params, GigyaCallback callback, GigyaInterceptionCallback interceptor) {
        params.put("ApiKey", configuration.getApiKey());
        return new GigyaRequestBuilderOld<GigyaAccount>(configuration)
                .sessionManager(sessionManager)
                .api(API)
                .httpMethod(Request.Method.GET)
                .params(params)
                .output(clazz)
                .callback(callback)
                .build();
    }

    public void call(Map<String, Object> params, final GigyaCallback callback, final GigyaInterceptionCallback interceptor) {
        GigyaRequest gigyaRequest = new GigyaRequestBuilder(configuration)
                .api(API)
                .params(params)
                .httpMethod(NetworkAdapter.Method.GET)
                .sessionManager(sessionManager)
                .build();
        networkAdapter.send(gigyaRequest, new INetworkCallbacks() {
            @Override
            public void onResponse(String jsonResponse) {
                if (callback == null) {
                    return;
                }
                try {
                    final GigyaResponse response = new GigyaResponse(new JSONObject(jsonResponse));
                    final int statusCode = response.getStatusCode();
                    Gson gson = new Gson();
                    if (statusCode == OK) {
                        /* Update session info */
                        if (response.contains("sessionInfo") && sessionManager != null) {
                            SessionInfo session = response.getField("sessionInfo", SessionInfo.class);
                            sessionManager.setSession(session);
                        }
                        // To avoid writing a clone constructor.
                        if (interceptor != null) {
                            T interception = (T) gson.fromJson(jsonResponse, clazz != null ? clazz : GigyaAccount.class);
                            interceptor.intercept(interception);
                        }
                        T parsed = (T) gson.fromJson(jsonResponse, clazz != null ? clazz : GigyaAccount.class);
                        callback.onSuccess(parsed);
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
