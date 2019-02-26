package com.gigya.android.sdk.interruption;

import com.gigya.android.sdk.AccountManager;
import com.gigya.android.sdk.SessionManager;
import com.gigya.android.sdk.model.Configuration;
import com.gigya.android.sdk.network.adapter.NetworkAdapter;

public abstract class GigyaResolver {

    protected Configuration configuration;
    protected NetworkAdapter networkAdapter;
    protected SessionManager sessionManager;
    protected AccountManager accountManager;

    public GigyaResolver(Configuration configuration, NetworkAdapter networkAdapter, SessionManager sessionManager, AccountManager accountManager) {
        this.configuration = configuration;
        this.networkAdapter = networkAdapter;
        this.sessionManager = sessionManager;
        this.accountManager = accountManager;
    }

    public static final String TFA_PHONE = "TFA_PHONE";
    public static final String TFA_TOTP = "TFA_TOTP";
    public static final String LINK_ACCOUNTS = "LINK_ACCOUNTS";
}
