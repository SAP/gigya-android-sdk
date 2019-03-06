package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.network.GigyaError;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.lang.ref.SoftReference;

public abstract class GigyaResolver<T> {

    protected NetworkAdapter networkAdapter;
    protected SessionManager sessionManager;
    protected AccountManager accountManager;

    final protected SoftReference<GigyaLoginCallback<T>> loginCallback;

    public GigyaResolver(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager, GigyaLoginCallback<T> loginCallback) {
        this.networkAdapter = networkAdapter;
        this.sessionManager = sessionManager;
        this.accountManager = accountManager;
        this.loginCallback = new SoftReference<>(loginCallback);
    }

    public abstract void cancel();

    protected void forwardError(GigyaError error) {
        if (loginCallback.get() != null)
            loginCallback.get().onError(error);
    }
}
