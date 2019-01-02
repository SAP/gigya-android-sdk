package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.GigyaRequestQueue;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public abstract class BaseApi<T> {

    @NonNull
    protected NetworkAdapter networkAdapter;

    @NonNull
    protected Configuration configuration;

    @Nullable
    protected SessionManager sessionManager;

    @Nullable
    protected GigyaRequestQueue requestQueue;

    @Nullable
    protected Class<T> clazz;

    //region Convenience constructors

    public BaseApi(@NonNull Configuration configuration) {
        this.configuration = configuration;
    }

    public BaseApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter) {
        this.configuration = configuration;
        this.networkAdapter = networkAdapter;
    }

    public BaseApi(@NonNull Configuration configuration, @Nullable Class<T> clazz) {
        this.configuration = configuration;
        this.clazz = clazz;
    }

    public BaseApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable Class<T> clazz) {
        this.configuration = configuration;
        this.networkAdapter = networkAdapter;
        this.clazz = clazz;
    }

    public BaseApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager) {
        this.configuration = configuration;
        this.sessionManager = sessionManager;
    }

    public BaseApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager) {
        this.configuration = configuration;
        this.networkAdapter = networkAdapter;
        this.sessionManager = sessionManager;
    }

    public BaseApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable Class<T> clazz) {
        this.configuration = configuration;
        this.sessionManager = sessionManager;
        this.clazz = clazz;
    }

    public BaseApi(@NonNull Configuration configuration, @NonNull NetworkAdapter networkAdapter, @Nullable SessionManager sessionManager, @Nullable Class<T> clazz) {
        this.configuration = configuration;
        this.networkAdapter = networkAdapter;
        this.sessionManager = sessionManager;
        this.clazz = clazz;
    }

    public BaseApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable GigyaRequestQueue requestQueue) {
        this.configuration = configuration;
        this.sessionManager = sessionManager;
        this.requestQueue = requestQueue;
    }

    public BaseApi(@NonNull Configuration configuration, @Nullable SessionManager sessionManager, @Nullable GigyaRequestQueue requestQueue, @Nullable Class<T> clazz) {
        this.configuration = configuration;
        this.sessionManager = sessionManager;
        this.requestQueue = requestQueue;
        this.clazz = clazz;
    }

    //endregion
}
