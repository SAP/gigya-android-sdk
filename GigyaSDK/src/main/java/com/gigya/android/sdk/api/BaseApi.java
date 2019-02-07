package com.gigya.android.sdk.api;

import android.support.annotation.Nullable;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.DependencyRegistry;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public abstract class BaseApi<T> {

    protected NetworkAdapter networkAdapter;
    protected Configuration configuration;
    protected SessionManager sessionManager;
    protected AccountManager accountManager;

    @Nullable
    protected Class<T> clazz;

    //region Convenience constructors

    public void inject(Configuration configuration, NetworkAdapter networkAdapter, SessionManager sessionManager,
                       AccountManager accountManager) {
        this.configuration = configuration;
        this.networkAdapter = networkAdapter;
        this.sessionManager = sessionManager;
        this.accountManager = accountManager;
    }

    BaseApi() {
        DependencyRegistry.getInstance().inject(this);
    }

    BaseApi(@Nullable Class<T> clazz) {
        DependencyRegistry.getInstance().inject(this);
        this.clazz = clazz;
    }

    //endregion
}
