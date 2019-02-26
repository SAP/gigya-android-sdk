package com.gigya.android.sdk.interruption.tfa;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.GigyaLoginCallback;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.interruption.GigyaResolver;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

import java.lang.ref.SoftReference;

public class TFATotpResolver<T> extends GigyaResolver {

    private SoftReference<GigyaLoginCallback<T>> loginCallback;
    private final String regToken;

    public TFATotpResolver(NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager,
                           String regToken, SoftReference<GigyaLoginCallback<T>> loginCallback) {
        super(networkAdapter, sessionManager, accountManager);
        this.regToken = regToken;
        this.loginCallback = loginCallback;
    }

    public void register() {

    }

    public void verify() {

    }
}
