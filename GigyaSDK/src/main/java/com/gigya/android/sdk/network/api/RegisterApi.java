package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.GigyaRegisterCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequestBuilderOld;
import com.gigya.android.sdk.network.GigyaRequestOld;
import com.gigya.android.sdk.network.GigyaRequestQueue;

import java.util.Map;

public class RegisterApi<T> extends BaseApi<T> implements IApi {

    public enum RegisterPolicy {
        EMAIL, USERNAME, EMAIL_OR_USERNAME
    }

    private static final String API_INIT_REGISTRATION = "accounts.initRegistration";
    private static final String API_REGISTER = "accounts.register";
    private static final String API_FINALIZE_REGISTRATION = "accounts.finalizeRegistration";

    private final boolean finalize;
    private final RegisterPolicy policy;

    public RegisterApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable GigyaRequestQueue requestQueue, @Nullable Class<T> clazz,
                       RegisterPolicy policy,
                       boolean finalize) {
        super(configuration, sessionManager, requestQueue, clazz);
        this.finalize = finalize;
        this.policy = policy;
    }

    private void updateRegisterPolicy(Map<String, Object> params) {
        final String loginId = (String) params.get("loginID");
        if (loginId == null) {
            return;
        }
        params.remove("loginID");
        switch (this.policy) {
            case EMAIL:
                params.put("email", loginId);
                break;
            case USERNAME:
            case EMAIL_OR_USERNAME:
                params.put("username", loginId);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public GigyaRequestOld getRequest(final Map<String, Object> params, final GigyaCallback callback, final GigyaInterceptionCallback interceptor) {
        updateRegisterPolicy(params);
        return new GigyaRequestBuilderOld<InitRegistration>(configuration)
                .sessionManager(sessionManager)
                .api(API_INIT_REGISTRATION)
                .output(InitRegistration.class)
                .callback(new GigyaCallback<InitRegistration>() {
                    @Override
                    public void onSuccess(InitRegistration obj) {
                        final String regToken = obj.getRegToken();
                        params.put("regToken", regToken);
                        params.put("finalizeRegistration", finalize);
                        sendRegistration(configuration, params, callback, interceptor);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        callback.onError(error);
                    }
                })
                .build();
    }

    @SuppressWarnings("unchecked")
    private <T> void sendRegistration(final Configuration configuration, final Map<String, Object> params, final GigyaCallback<T> callback, final GigyaInterceptionCallback<T> interceptor) {
        final GigyaRequestOld request = new GigyaRequestBuilderOld(configuration)
                .sessionManager(sessionManager)
                .api(API_REGISTER)
                .params(params)
                .output(clazz)
                .interceptor(interceptor)
                .callback(callback)
                .callback(new GigyaRegisterCallback<T>() {

                    @Override
                    public void onSuccess(T obj) {
                        params.clear();
                        callback.onSuccess(obj);
                    }

                    @Override
                    public void onError(GigyaError error) {
                        onRegistrationError(error, params, interceptor, callback);
                    }
                })
                .build();
        requestQueue.add(request);
    }

    @SuppressWarnings("unchecked")
    private <T> void onRegistrationError(GigyaError error, final Map<String, Object> params, GigyaInterceptionCallback<T> interceptor, GigyaCallback<T> callback) {
        final int errorCode = error.getErrorCode();
        switch (errorCode) {
            case GigyaError.Codes.ERROR_ACCOUNT_PENDING_REGISTRATION:
                ApiResolver<T> resolver = new ApiResolver.Builder()
                        .incident(ApiResolver.Incident.ACCOUNT_PENDING_REGISTRATION)
                        .queue(requestQueue)
                        .sessionMananger(sessionManager)
                        .interceptor(interceptor)
                        .params(params)
                        .callback(callback)
                        .build();
                final String regToken = (String) params.get("regToken");
                ((GigyaRegisterCallback) callback).onPendingRegistration(regToken, resolver);
                break;
            default:
                callback.onError(error);
                break;
        }
    }

    //region Flow specific classes

    private static class InitRegistration {

        private String regToken;

        public String getRegToken() {
            return regToken;
        }
    }

    //endregion
}
