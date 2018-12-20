package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.BaseGigyaAccount;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaRequestQueue;

public abstract class BaseApi<T> {

    @NonNull
    protected Configuration configuration;

    @Nullable
    protected SessionManager sessionManager;

    @Nullable
    protected GigyaRequestQueue requestQueue;

    @Nullable
    protected Class<? extends BaseGigyaAccount> clazz;

    //region Convenience constructors

    public BaseApi(@NonNull Configuration configuration) {
        this.configuration = configuration;
    }

    public BaseApi(@NonNull Configuration configuration, @Nullable Class<? extends BaseGigyaAccount> clazz) {
        this.configuration = configuration;
        this.clazz = clazz;
    }

    public BaseApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager) {
        this.configuration = configuration;
        this.sessionManager = sessionManager;
    }

    public BaseApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable Class<? extends BaseGigyaAccount> clazz) {
        this.configuration = configuration;
        this.sessionManager = sessionManager;
        this.clazz = clazz;
    }

    public BaseApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable GigyaRequestQueue requestQueue) {
        this.configuration = configuration;
        this.sessionManager = sessionManager;
        this.requestQueue = requestQueue;
    }

    public BaseApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable GigyaRequestQueue requestQueue, @Nullable Class<? extends BaseGigyaAccount> clazz) {
        this.configuration = configuration;
        this.sessionManager = sessionManager;
        this.requestQueue = requestQueue;
        this.clazz = clazz;
    }

    //endregion
}
