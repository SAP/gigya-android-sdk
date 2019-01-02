package com.gigya.android.sdk.network.api;

import com.gigya.android.sdk.GigyaCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.GigyaInterceptionCallback;
import com.gigya.android.sdk.network.GigyaRequestBuilderOld;
import com.gigya.android.sdk.network.GigyaRequestOld;
import com.gigya.android.sdk.network.GigyaRequestQueue;

import java.util.Map;

public class ApiResolver<T> {

    public enum Incident {
        ACCOUNT_PENDING_REGISTRATION
    }

    private Configuration configuration;
    private SessionManager sessionManager;
    private GigyaRequestQueue requestQueue;
    private Incident incident;
    private Class<T> clazz;
    private Map<String, Object> params;
    private GigyaInterceptionCallback<T> interceptor;
    private GigyaCallback<T> callback;

    public void setParameter(String key, Object value) {
        params.put(key, value);
    }

    public void resolve() {
        switch (incident) {
            case ACCOUNT_PENDING_REGISTRATION:
                finalizeRegistration();
                break;
        }
    }

    private void flush() {
        configuration = null;
        sessionManager = null;
        incident = null;
        clazz = null;
        params = null;
        interceptor = null;
        callback = null;
    }

    //region Builder

    public static class Builder {

        private ApiResolver resolver = new ApiResolver();

        public Builder configuration(Configuration configuration) {
            resolver.configuration = configuration;
            return this;
        }

        public Builder sessionMananger(SessionManager sessionManager) {
            resolver.sessionManager = sessionManager;
            return this;
        }

        public Builder queue(GigyaRequestQueue queue) {
            resolver.requestQueue = queue;
            return this;
        }

        public <T> Builder clazz(Class<T> clazz) {
            resolver.clazz = clazz;
            return this;
        }

        public Builder incident(Incident incident) {
            resolver.incident = incident;
            return this;
        }

        public <T> Builder interceptor(GigyaInterceptionCallback<T> interceptor) {
            resolver.interceptor = interceptor;
            return this;
        }

        public <T> Builder callback(GigyaCallback<T> callback) {
            resolver.callback = callback;
            return this;
        }

        public Builder params(Map<String, Object> params) {
            resolver.params = params;
            return this;
        }

        public ApiResolver build() {
            return resolver;
        }
    }

    //endregion

    //region Business Apis

    @SuppressWarnings("unchecked")
    private void finalizeRegistration() {
        final String regToken = (String) params.get("regToken");
        if (regToken == null || regToken.isEmpty()) {
            return;
        }
        GigyaRequestOld request = new GigyaRequestBuilderOld<>(configuration)
                .sessionManager(sessionManager)
                .api("accounts.accounts.finalizeRegistration")
                .interceptor(interceptor)
                .params(params)
                .output(clazz)
                .callback(new GigyaCallback<T>() {
                    @Override
                    public void onSuccess(T obj) {
                        if (callback != null) {
                            callback.onSuccess(obj);
                        }
                        flush();
                    }

                    @Override
                    public void onError(GigyaError error) {
                        if (callback != null) {
                            callback.onError(error);
                        }
                        flush();
                    }
                }).build();
        requestQueue.add(request);
    }

    //endregion
}
