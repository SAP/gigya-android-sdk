package com.gigya.android.sdk.network;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.SessionInfo;
import com.google.gson.Gson;

import org.json.JSONObject;

/*
Custom response handler.
 */
class GigyaResponseHandler<T> {

    private static final int OK = 200;
    private final Gson gson = new Gson();
    private final GigyaCallback<T> callback;

    @Nullable
    private final GigyaInterceptionCallback interceptor;

    @Nullable
    private final Class<T> clazz;

    @Nullable
    private SessionManager sessionManager;

    GigyaResponseHandler(GigyaCallback<T> callback,
                         @Nullable Class<T> clazz,
                         @Nullable SessionManager sessionManager,
                         @Nullable GigyaInterceptionCallback iterceptor) {
        this.callback = callback;
        this.clazz = clazz;
        this.sessionManager = sessionManager;
        this.interceptor = iterceptor;
    }

    void verifyWith(JSONObject jsonResponse) {
        final GigyaResponse response = new GigyaResponse(jsonResponse);
        final int statusCode = response.getStatusCode();

        if (statusCode == OK) {
            /*
            Check if response contains any session info. If so handle it accordingly.
             */
            if (response.contains("sessionInfo") && this.sessionManager != null) {
                SessionInfo session = response.getField("sessionInfo", SessionInfo.class);
                this.sessionManager.setSession(session);
            }

            postSuccess(response);
            return;
        }

        // Handle error codes.
        final int errorCode = response.getErrorCode();
        boolean handled = false;

        // TODO: 04/12/2018 Update when starting to handle errors.
        if (!handled) {
            postError(response);
        }
    }

    //region POST Success/Error

    @SuppressWarnings("unchecked")
    private void postSuccess(GigyaResponse gigyaResponse) {
        final T response = clazz == null ? (T) gigyaResponse : gson.fromJson(gigyaResponse.asJson(), clazz);

        // Intercept response if needed.
        if (this.interceptor != null) {
            this.interceptor.intercept(response);
        }

        if (this.callback != null) {
            this.callback.onSuccess(response);
        }
    }

    private void postError(GigyaResponse response) {
        if (this.callback != null) {
            final int errorCode = response.getErrorCode();
            final String localizedMessage = response.getErrorDetails();
            final String callId = response.getCallId();
            this.callback.onError(new GigyaError(errorCode, localizedMessage, callId));
        }
    }

    //endregion

}
