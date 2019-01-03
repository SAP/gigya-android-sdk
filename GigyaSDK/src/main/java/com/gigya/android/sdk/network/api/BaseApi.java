package com.gigya.android.sdk.network.api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public abstract class BaseApi<T> {

    @NonNull
    protected NetworkAdapter networkAdapter;

    @NonNull
    protected Configuration configuration;

    @Nullable
    protected SessionManager sessionManager;

    @Nullable
    protected Class<T> clazz;

    //region Convenience constructors

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

    //endregion
}
